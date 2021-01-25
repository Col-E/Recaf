package me.coley.recaf.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link LoggerFactory} wrapper that lets us intercept all logged messages.
 *
 * @author Matt Coley
 */
public class Logging {
	private static final Map<String, Logger> loggers = new HashMap<>();
	private static final List<LogConsumer<String>> logMessageConsumers = new ArrayList<>();
	private static final List<LogConsumer<Throwable>> logExceptionConsumers = new ArrayList<>();

	/**
	 * @param name
	 * 		Logger name.
	 *
	 * @return Logger associated with name.
	 */
	public static Logger get(String name) {
		return loggers.computeIfAbsent(name, k -> intercept(name, getLogger(name)));
	}

	/**
	 * @param cls
	 * 		Logger class key.
	 *
	 * @return Logger associated with class.
	 */
	public static Logger get(Class<?> cls) {
		return loggers.computeIfAbsent(cls.getName(), k -> intercept(cls.getName(), getLogger(cls)));
	}

	/**
	 * @param consumer
	 * 		New log message consumer.
	 */
	public static void addLogMessageConsumer(LogConsumer<String> consumer) {
		logMessageConsumers.add(consumer);
	}

	/**
	 * @param consumer
	 * 		Log message consumer to remove.
	 */
	public static void removeLogMessageConsumer(LogConsumer<String> consumer) {
		logMessageConsumers.remove(consumer);
	}

	/**
	 * @param consumer
	 * 		New log exception consumer.
	 */
	public static void addLogExceptionConsumer(LogConsumer<Throwable> consumer) {
		logExceptionConsumers.add(consumer);
	}

	/**
	 * @param consumer
	 * 		Log exception consumer to remove.
	 */
	public static void removeLogExceptionConsumer(LogConsumer<Throwable> consumer) {
		logExceptionConsumers.remove(consumer);
	}

	private static Logger intercept(String name, Logger logger) {
		return new InterceptingLogger(logger) {
			@Override
			public void intercept(Level level, String message) {
				logMessageConsumers.forEach(consumer -> consumer.accept(name, level, message));
			}

			@Override
			public void intercept(Level level, Throwable t) {
				logExceptionConsumers.forEach(consumer -> consumer.accept(name, level, t));
			}
		};
	}
}
