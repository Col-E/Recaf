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
}
