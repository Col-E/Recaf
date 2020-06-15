package me.coley.recaf.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * String parsing &amp; manipulation utilities.
 *
 * @author Matt
 */
public class StringUtil {
	private static final Map<String, String> INPUT_TO_ESCAPED = new HashMap<>();
	private static final Map<String, String> ESCAPED_TO_OUTPUT = new HashMap<>();

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
		while(index > charz.length - 1) {
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
		for(int i = 0; i < level; i++)
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
	 * Escape whitespace characters
	 *
	 * @param s
	 * 		Text to escape.
	 *
	 * @return Whitespace free text.
	 */
	public static String escape(String s) {
		for (Map.Entry<String, String> e : INPUT_TO_ESCAPED.entrySet())
			s = s.replace(e.getKey(), e.getValue());
		return s;
	}

	/**
	 * Unescape escaped characters.
	 *
	 * @param s
	 * 		Text to unescape.
	 *
	 * @return Text with whitespaces put back.
	 */
	public static String unescape(String s) {
		for (Map.Entry<String, String> e : ESCAPED_TO_OUTPUT.entrySet())
			s = s.replace(e.getKey(), e.getValue());
		return s;
	}

	private static void addEscape(String in, String out) {
		INPUT_TO_ESCAPED.put(in, out);
		ESCAPED_TO_OUTPUT.put(out, in);
	}

	static {
		//Unicode whitespaces
		for (int i = 0; i < 0x20; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x7F; i < 0xA0; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x6E5; i < 0x6E6; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x17B4; i < 0x17B5; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x180B; i < 0x180E; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x2000; i < 0x200F; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x2028; i < 0x202F; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		for (int i = 0x205F; i < 0x206F; i++)
			addEscape(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		addEscape(String.valueOf(Character.toChars('\u3164')), "\\u" + String.format("%04X", (int) '\u3164'));
		addEscape(String.valueOf(Character.toChars('\u318F')), "\\u" + String.format("%04X", (int) '\u318F'));
		// Standard escapes
		addEscape("\r", "\\r");
		addEscape("\n", "\\n");
		addEscape("\t", "\\t");
	}
}
