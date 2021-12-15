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
	 * Creates a string incrementing in numerical value.
	 * Example: a, b, c, ... z, aa, ab ...
	 *
	 * @param alphabet
	 * 		The alphabet to pull from.
	 * @param index
	 * 		Name index.
	 *
	 * @return Generated String
	 */
	public static String generateName(String alphabet, int index) {
		// String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		char[] charz = alphabet.toCharArray();
		int alphabetLength = charz.length;
		int m = 8;
		final char[] array = new char[m];
		int n = m - 1;
		while (index > charz.length - 1) {
			int k = Math.abs(-(index % alphabetLength));
			array[n--] = charz[k];
			index /= alphabetLength;
			index -= 1;
		}
		array[n] = charz[index];
		return new String(array, n, m - n);
	}

	/**
	 * @param level
	 * 		Level of indent.
	 * @param indent
	 * 		Indent format.
	 *
	 * @return Indented string.
	 */
	public static String indent(int level, String indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; i++)
			sb.append(indent);
		return sb.toString();
	}

	/**
	 * @param str
	 * 		Original string.
	 * @param maxLength
	 * 		Maximum length.
	 *
	 * @return String, cutting off anything past the maximum length if necessary.
	 */
	public static String limit(String str, int maxLength) {
		if (str.length() > maxLength)
			return str.substring(0, maxLength);
		return str;
	}

	/**
	 * Convert an enum to a string.
	 *
	 * @param value
	 * 		Enum value.
	 * @param <E>
	 * 		Type of enum.
	 *
	 * @return Case modified name of enum.
	 */
	public static <E extends Enum<?>> String toString(E value) {
		return value.name().substring(0, 1).toUpperCase() + value.name().substring(1).toLowerCase();
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
