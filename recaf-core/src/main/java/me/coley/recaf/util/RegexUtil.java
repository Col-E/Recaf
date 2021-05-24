package me.coley.recaf.util;

import jregex.Matcher;
import jregex.Pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Some common regular expression functions and cache for compiled patterns.
 *
 * @author Matt Coley
 */
public class RegexUtil {
	private static final Map<String, Pattern> PATTERNS = new HashMap<>();

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
	 * @param pattern pattern
	 * @param input an input to verify
	 *
	 * @return {@code true} if input matches.
	 */
	public static boolean matches(String pattern, String input) {
		return pattern(pattern).matches(input);
	}

	/**
	 * Creates new {@link Pattern} or gets it from cache.
	 *
	 * @param regex pattern's regex
	 * @return {@link Pattern}
	 */
	public static Pattern pattern(String regex) {
		return PATTERNS.computeIfAbsent(regex, Pattern::new);
	}
}
