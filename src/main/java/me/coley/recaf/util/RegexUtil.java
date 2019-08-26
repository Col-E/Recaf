package me.coley.recaf.util;

import jregex.Matcher;
import jregex.Pattern;

/**
 * Misc regex patterns.
 *
 * @author Matt
 */
public class RegexUtil {
	private static final Pattern WORD = new Pattern("\\s*(\\S+)\\s*");

	/**
	 * @param text
	 * 		Some text containing at least one word character.
	 *
	 * @return The first sequence of connected word characters. {@code null} if no word characters
	 * are found.
	 */
	public static String getFirstWord(String text) {
		Matcher m = WORD.matcher(text);
		if(m.find())
			return m.group(1);
		return null;
	}

	/**
	 * @param pattern
	 * 		Pattern to match.
	 * @param text
	 * 		Some text containing at a match for the given pattern.
	 *
	 * @return First matching sequence from the text.
	 */
	public static String getFirstToken(String pattern, String text) {
		Matcher m = getMatcher(pattern, text);
		if(m.find())
			return m.group(0);
		return null;
	}

	/**
	 * @param pattern
	 * 		Pattern to match.
	 * @param text
	 * 		Some text containing at a match for the given pattern.
	 *
	 * @return Text matcher.
	 */
	public static Matcher getMatcher(String pattern, String text) {
		return new Pattern(pattern).matcher(text);
	}
}
