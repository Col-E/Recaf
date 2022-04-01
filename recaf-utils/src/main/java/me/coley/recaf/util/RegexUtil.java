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
	private static final Map<String, Pattern> PATTERNS = new HashMap<>();
	private static final Pattern INVALID_PATTERN = new Pattern("^$");

	/**
	 * @param pattern
	 * 		Pattern to match.
	 * @param text
	 * 		Some text containing a match for the given pattern.
	 *
	 * @return Text matcher.
	 */
	public static Matcher getMatcher(String pattern, String text) {
		return pattern(pattern).matcher(text);
	}

	/**
	 * Checks if the entire input matches a pattern.
	 *
	 * @param pattern
	 * 		pattern
	 * @param input
	 * 		an input to verify
	 *
	 * @return {@code true} if input matches.
	 */
	public static boolean matches(String pattern, String input) {
		return pattern(pattern).matches(input);
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
		return PATTERNS.computeIfAbsent(regex, RegexUtil::generate);
	}

	/**
	 * @param regex
	 * 		Regular expression to compile.
	 *
	 * @return Compiled pattern, or {@link #INVALID_PATTERN} if the pattern is invalid.
	 */
	private static Pattern generate(String regex) {
		if (FAILED_PATTERNS.contains(regex))
			return INVALID_PATTERN;
		try {
			return new Pattern(regex);
		} catch (Throwable t) {
			FAILED_PATTERNS.add(regex);
			logger.error("Invalid regex pattern", t);
			return INVALID_PATTERN;
		}
	}
}
