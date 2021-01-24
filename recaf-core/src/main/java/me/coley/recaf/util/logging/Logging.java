package me.coley.recaf.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link LoggerFactory} wrapper that lets us intercept all logged messages.
 *
 * @author Matt Coley
 */
public class Logging {
	private static final Map<String, Logger> loggers = new HashMap<>();

	/**
	 * @param name
	 * 		Logger name.
	 *
	 * @return Logger associated with name.
	 */
	public static Logger get(String name) {
		return loggers.computeIfAbsent(name, k -> intercept(name, LoggerFactory.getLogger(name)));
	}

	/**
	 * @param cls
	 * 		Logger class key.
	 *
	 * @return Logger associated with class.
	 */
	public static Logger get(Class<?> cls) {
		return loggers.computeIfAbsent(cls.getName(), k -> intercept(cls.getName(), LoggerFactory.getLogger(cls)));
	}

	private static Logger intercept(String name, Logger logger) {
		// TODO: Log to file
		// TODO: Allow foreign classes to access logging in some way, probably like this:
		//  - List of BiConsumer<Level, String>
		//  - List of BiConsumer<Level, Throwable>
		return new InterceptingLogger(logger) {
			@Override
			public void log(Level level, String message) {

			}

			@Override
			public void log(Level level, Throwable t) {

			}
		};
	}
}
