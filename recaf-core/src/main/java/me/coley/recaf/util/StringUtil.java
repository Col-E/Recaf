package me.coley.recaf.util;

/**
 * Various utilities for {@link String} manipulation.
 *
 * @author Matt Coley
 */
public class StringUtil {
	/**
	 * Replace the last match of some text in the given string.
	 *
	 * @param string
	 * 		Text containing items to replace.
	 * @param toReplace
	 * 		Pattern to match.
	 * @param replacement
	 * 		Text to replace pattern with.
	 *
	 * @return Modified string.
	 */
	public static String replaceLast(String string, String toReplace, String replacement) {
		int i = string.lastIndexOf(toReplace);
		if (i > -1)
			return string.substring(0, i) + replacement + string.substring(i + toReplace.length());
		return string;
	}

	/**
	 * @param pattern
	 * 		Pattern to look for.
	 * @param text
	 * 		Text to check.
	 *
	 * @return Number of times the given pattern appears in the text.
	 */
	public static int count(String pattern, String text) {
		int count = 0;
		while (text.contains(pattern)) {
			text = text.replaceFirst(pattern, "");
			count++;
		}
		return count;
	}
}
