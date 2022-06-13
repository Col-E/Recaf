package me.coley.recaf.util.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import me.coley.recaf.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link LoggerFactory} wrapper that lets us intercept all logged messages.
 *
 * @author Matt Coley
 */
public class Logging {
	private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
	private static final List<LogConsumer<String>> logConsumers = new ArrayList<>();
	private static Level interceptLevel = Level.INFO;

	/**
	 * @param name
	 * 		Logger name.
	 *
	 * @return Logger associated with name.
	 */
	public static Logger get(String name) {
		return loggers.computeIfAbsent(name, k -> intercept(k, getLogger(k)));
	}

	/**
	 * @param cls
	 * 		Logger class key.
	 *
	 * @return Logger associated with class.
	 */
	public static Logger get(Class<?> cls) {
		return loggers.computeIfAbsent(cls.getName(), k -> intercept(k, getLogger(k)));
	}

	/**
	 * @param consumer
	 * 		New log message consumer.
	 */
	public static void addLogConsumer(LogConsumer<String> consumer) {
		logConsumers.add(consumer);
	}

	/**
	 * @param consumer
	 * 		Log message consumer to remove.
	 */
	public static void removeLogConsumer(LogConsumer<String> consumer) {
		logConsumers.remove(consumer);
	}

	/**
	 * Sets the target level for log interception. This affects what messages {@link LogConsumer}s receive.
	 *
	 * @param level
	 * 		New target level.
	 */
	public static void setInterceptLevel(Level level) {
		interceptLevel = level;
	}

	/**
	 * Registers a file appender for all log calls.
	 *
	 * @param path
	 * 		Path to file to append to.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void addFileAppender(Path path) {
		// We do it this way so the file path can be set at runtime.
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		FileAppender fileAppender = new FileAppender<>();
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

	/**
	 * For some reason, gradle's unit tests do NOT print any {@link Logger} call to sys-out.
	 * This is a dumb fix for that.
	 */
	private static void addTestPhaseLogging() {
		try {
			// Check if JUnit exists, if so, we need to fix logging.
			// If somebody figures out a better approach, please open a PR.
			Class.forName("org.junit.jupiter.api.Assertions");
			addLogConsumer(new LogConsumer<>() {
				@Override
				public void accept(String loggerName, Level level, String messageContent) {
					loggerName = StringUtil.shortenPath(loggerName.replace('.', '/'));
					System.out.println("[" + loggerName + "-" + level.name() + "] " + messageContent);
				}

				@Override
				public void accept(String loggerName, Level level, String messageContent, Throwable throwable) {
					loggerName = StringUtil.shortenPath(loggerName.replace('.', '/'));
					System.err.println("[" + loggerName + "-" + level.name() + "] " + messageContent);
					throwable.printStackTrace();
				}
			});
		} catch (ClassNotFoundException ignored) {
			// expected
		}
	}

	private static Logger intercept(String name, Logger logger) {
		return new InterceptingLogger(logger) {
			@Override
			public void intercept(Level level, String message) {
				if (interceptLevel.toInt() <= level.toInt())
					logConsumers.forEach(consumer -> consumer.accept(name, level, message));
			}

			@Override
			public void intercept(Level level, String message, Throwable t) {
				if (interceptLevel.toInt() <= level.toInt())
					logConsumers.forEach(consumer -> consumer.accept(name, level, message, t));
			}
		};
	}

	static {
		addTestPhaseLogging();
	}
}
