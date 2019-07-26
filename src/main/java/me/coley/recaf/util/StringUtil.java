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
		return input.split("[\\r\\n]+");
	}
}
