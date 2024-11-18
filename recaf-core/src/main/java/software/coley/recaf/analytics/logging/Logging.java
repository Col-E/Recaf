package software.coley.recaf.analytics.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link LoggerFactory} wrapper that lets us intercept all logged messages.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Logging not relevant for test coverage")
public class Logging {
	private static final Map<String, DebuggingLogger> loggers = new ConcurrentHashMap<>();
	private static final NavigableSet<String> loggerKeys = Collections.synchronizedNavigableSet(new TreeSet<>());
	private static final List<LogConsumer<String>> logConsumers = new CopyOnWriteArrayList<>();
	private static Level interceptLevel = Level.INFO;

	/**
	 * @return Set of the current logger keys.
	 */
	@Nonnull
	public static NavigableSet<String> loggerKeys() {
		// We track the keys in this separate set so that we can retrieve them
		// in sorted order without needing to wrap in 'new TreeSet' every time.
		return Collections.unmodifiableNavigableSet(loggerKeys);
	}

	/**
	 * @param name
	 * 		Logger name.
	 *
	 * @return Logger associated with name.
	 */
	@Nonnull
	public static DebuggingLogger get(@Nonnull String name) {
		return loggers.computeIfAbsent(name, k -> intercept(k, getLogger(k)));
	}

	/**
	 * @param cls
	 * 		Logger class key.
	 *
	 * @return Logger associated with class.
	 */
	@Nonnull
	public static DebuggingLogger get(@Nonnull Class<?> cls) {
		return loggers.computeIfAbsent(cls.getName(), k -> intercept(k, getLogger(k)));
	}

	/**
	 * @param consumer
	 * 		New log message consumer.
	 */
	public static void addLogConsumer(@Nonnull LogConsumer<String> consumer) {
		logConsumers.add(consumer);
	}

	/**
	 * @param consumer
	 * 		Log message consumer to remove.
	 */
	public static void removeLogConsumer(@Nonnull LogConsumer<String> consumer) {
		logConsumers.remove(consumer);
	}

	/**
	 * Sets the target level for log interception. This affects what messages {@link LogConsumer}s receive.
	 *
	 * @param level
	 * 		New target level.
	 */
	public static void setInterceptLevel(@Nonnull Level level) {
		interceptLevel = level;
	}

	/**
	 * Registers a file appender for all log calls.
	 *
	 * @param path
	 * 		Path to file to append to.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void addFileAppender(@Nonnull Path path) {
		// We do it this way so the file path can be set at runtime.
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		FileAppender fileAppender = new FileAppender<>();
		fileAppender.addFilter(new RecafLoggingFilter());
		fileAppender.setFile(path.toString());
		fileAppender.setContext(loggerContext);
		fileAppender.setPrudent(true);
		fileAppender.setAppend(true);
		fileAppender.setImmediateFlush(true);

		// Pattern
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%d{HH:mm:ss.SSS} [%logger{0}/%thread] %-5level: %msg%n");
		encoder.start();
		fileAppender.setEncoder(encoder);

		// Start file appender
		fileAppender.start();

		// Create logger
		ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger)
				LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		logbackLogger.addAppender(fileAppender);
		logbackLogger.setAdditive(false);
	}

	@Nonnull
	private static DebuggingLogger intercept(@Nonnull String name, @Nonnull Logger logger) {
		loggerKeys.add(name);
		return new InterceptingLogger(logger) {
			@Override
			public void intercept(@Nonnull Level level, String message) {
				if (interceptLevel.toInt() <= level.toInt())
					logConsumers.forEach(consumer -> consumer.accept(name, level, message));
			}

			@Override
			public void intercept(@Nonnull Level level, String message, Throwable t) {
				if (interceptLevel.toInt() <= level.toInt())
					logConsumers.forEach(consumer -> consumer.accept(name, level, message, t));
			}
		};
	}
}
