package me.coley.recaf.util;

import me.coley.recaf.util.struct.Pair;
import org.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * Proxy to intercept tinylog logging.
 *
 * @author Matt
 */
public class Log {
	/**
	 * Set of consumers that are fed trace-level messages.
	 */
	public static final Set<Consumer<String>> traceConsumers = new HashSet<>();
	/**
	 * Set of consumers that are fed debug-level messages.
	 */
	public static final Set<Consumer<String>> debugConsumers = new HashSet<>();
	/**
	 * Set of consumers that are fed info-level messages.
	 */
	public static final Set<Consumer<String>> infoConsumers = new HashSet<>();
	/**
	 * Set of consumers that are fed warn-level messages.
	 */
	public static final Set<Consumer<String>> warnConsumers = new HashSet<>();
	/**
	 * Set of consumers that are fed error-level messages.
	 */
	public static final Set<Consumer<Pair<String, Throwable>>> errorConsumers = new HashSet<>();

	/**
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void trace(String msg, Object... args) {
		Logger.trace(msg, args);
		traceConsumers.forEach(c -> c.accept(compile(msg, args)));
	}

	/**
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void debug(String msg, Object... args) {
		Logger.debug(msg, args);
		debugConsumers.forEach(c -> c.accept(compile(msg, args)));
	}

	/**
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void info(String msg, Object... args) {
		Logger.info(msg, args);
		infoConsumers.forEach(c -> c.accept(compile(msg, args)));
	}

	/**
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void warn(String msg, Object... args) {
		Logger.warn(msg, args);
		warnConsumers.forEach(c -> c.accept(compile(msg, args)));
	}

	/**
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void error(String msg, Object... args) {
		error(null, msg, args);
	}

	/**
	 * @param t
	 * 		Exception to print.
	 * @param msg
	 * 		Message format.
	 * @param args
	 * 		Message arguments.
	 */
	public static void error(Throwable t, String msg, Object... args) {
		Logger.error(t, msg, args);
		errorConsumers.forEach(c -> c.accept(new Pair<>(compile(msg, args), t)));
	}

	/**
	 * Compiles message with "{}" arg patterns.
	 *
	 * @param msg
	 * 		Message pattern.
	 * @param args
	 * 		Values to pass.
	 *
	 * @return Compiled message with inlined arg values.
	 */
	private static String compile(String msg, Object[] args) {
		int c = 0;
		while(msg.contains("{}")) {
			Object arg = args[c];
			String argStr = arg == null ? "null" : arg.toString();
			msg = msg.replaceFirst("\\{}", Matcher.quoteReplacement(argStr));
			c++;
		}
		return msg;
	}
}
