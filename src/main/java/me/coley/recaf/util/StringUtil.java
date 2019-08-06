package me.coley.recaf.util;

/**
 * String parsing &amp; manipulation utilities.
 *
 * @author Matt
 */
public class StringUtil {
	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 */
	public static String[] splitNewline(String input) {
		return input.split("\\r\\n|\\n");
	}

	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 * Empty lines <i>(Containing only the newline split)</i> are omitted.
	 */
	public static String[] splitNewlineSkipEmpty(String input) {
		return input.split("[\\r\\n]+");
	}
}
