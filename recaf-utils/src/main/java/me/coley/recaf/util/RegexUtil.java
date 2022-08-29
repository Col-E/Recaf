package me.coley.recaf.util;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Some common regular expression functions and cache for compiled patterns.
 *
 * @author Matt Coley
 */
public class RegexUtil {
	private static final Logger logger = Logging.get(RegexUtil.class);
	private static final Set<String> FAILED_PATTERNS = new HashSet<>();
	private static final Map<String, ThreadLocalPattern> PATTERNS = new HashMap<>();
	private static final ThreadLocalPattern INVALID_PATTERN = new ThreadLocalPattern(new Pattern("^$"));

	/**
	 * @param pattern
	 * 		Pattern to match.
	 * @param text
	 * 		Some text containing a match for the given pattern.
	 *
	 * @return Text matcher.
	 */
	public static Matcher getMatcher(String pattern, String text) {
		return getPattern(pattern).matcher(text);
	}

	/**
	 * Checks if the entire input matches a pattern.
	 *
	 * @param pattern
	 * 		Pattern to match.
	 * @param input
	 * 		Text to match against.
	 *
	 * @return {@code true} when the input is a full match.
	 */
	public static boolean matches(String pattern, String input) {
		return getMatcher(pattern, input).matches();
	}

	/**
	 * Checks if any portion of the input matches a pattern.
	 *
	 * @param pattern
	 * 		Pattern to match.
	 * @param input
	 * 		Text to match against.
	 *
	 * @return {@code true} when any part of the input matches.
	 */
	public static boolean matchesAny(String pattern, String input) {
		return getMatcher(pattern, input).find();
	}

	/**
	 * Creates new {@link Pattern} or gets it from cache.
	 *
	 * @param regex
	 * 		Regular expression text.
	 *
	 * @return Compiled {@link Pattern} of the regular expression.
	 */
	public synchronized static Pattern pattern(String regex) {
		return getPattern(regex).pattern;
	}

	/**
	 * Creates new {@link ThreadLocalPattern} or gets it from cache.
	 *
	 * @param regex
	 * 		Regular expression text.
	 *
	 * @return Compiled {@link ThreadLocalPattern} of the regular expression.
	 */
	private synchronized static ThreadLocalPattern getPattern(String regex) {
		return PATTERNS.computeIfAbsent(regex, RegexUtil::generate);
	}

	/**
	 * @param regex
	 * 		Regular expression to compile.
	 *
	 * @return Compiled pattern, or {@link #INVALID_PATTERN} if the pattern is invalid.
	 */
	private static ThreadLocalPattern generate(String regex) {
		if (FAILED_PATTERNS.contains(regex))
			return INVALID_PATTERN;
		try {
			return new ThreadLocalPattern(new Pattern(regex));
		} catch (Throwable t) {
			FAILED_PATTERNS.add(regex);
			logger.error("Invalid regex pattern", t);
			return INVALID_PATTERN;
		}
	}

	private static final class ThreadLocalPattern {

		final ThreadLocal<Matcher> matcher = new ThreadLocal<>();
		final Pattern pattern;

		private ThreadLocalPattern(Pattern pattern) {
			this.pattern = pattern;
		}

		Matcher matcher(String input) {
			ThreadLocal<Matcher> tlc = this.matcher;
			Matcher matcher = tlc.get();
			if (matcher == null) {
				matcher = pattern.matcher(input);
				tlc.set(matcher);
			} else {
				matcher.setTarget(input);
			}
			return matcher;
		}
	}
}
