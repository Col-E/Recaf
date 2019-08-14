package me.coley.recaf.util;

import java.util.Arrays;

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
		String[] split = input.split("[\\r\\n]+");
		// If the first line of the file is a newline split will still have
		// one blank entry at the start.
		if (split[0].isEmpty())
			return Arrays.copyOfRange(split, 1, split.length);
		return split;
	}
}
