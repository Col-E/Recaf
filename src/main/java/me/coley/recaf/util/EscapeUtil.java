package me.coley.recaf.util;

import org.reactfx.util.TriFunction;

/**
 * Escape code replacement utility.
 *
 * @author xxDark
 */
public final class EscapeUtil {
	private EscapeUtil() { }

	/**
	 * Replaces any escape code with its literal value.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescape(String input) {
		return unescapeStandard(unescapeUnicode(input));
	}

	/**
	 * Replaces escaped unicode with actual unicode. For example: {@code \u0048}
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescapeUnicode(String input) {
		return unescape(input, EscapeUtil::escapeUnicode);
	}

	/**
	 * Replaces standard escape codes with literal values. For example: {@code \n}
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescapeStandard(String input) {
		return unescape(input, EscapeUtil::escapeStandard);
	}

	private static String unescape(String input, TriFunction<String, Integer, StringBuilder, Integer> c) {
		int len = input.length();
		int cursor = 0;
		StringBuilder builder = new StringBuilder(len);
		while(cursor < len) {
			int consumed = c.apply(input, cursor, builder);
			if(consumed == 0) {
				char c1 = input.charAt(cursor++);
				builder.append(c1);
				if(Character.isHighSurrogate(c1) && cursor < len) {
					char c2 = input.charAt(cursor);
					if(Character.isLowSurrogate(c2)) {
						builder.append(c2);
						cursor += 1;
					}
				}
			} else {
				for(int pt = 0; pt < consumed; ++pt)
					cursor += Character.charCount(Character.codePointAt(input, cursor));
			}
		}
		return builder.toString();
	}

	private static int escapeUnicode(String input, int cursor, StringBuilder builder) {
		if(input.charAt(cursor) != '\\')
			return 0;
		if(cursor + 1 >= input.length())
			return 0;
		if(input.charAt(cursor + 1) != 'u')
			return 0;
		int i;
		i = 2;
		while(cursor + i < input.length() && input.charAt(cursor + i) == 'u')
			i++;
		if(cursor + i < input.length() && input.charAt(cursor + i) == '+')
			i += 1;
		if(cursor + i + 4 <= input.length()) {
			String unicode = input.substring(cursor + i, cursor + i + 4);
			try {
				int value = Integer.parseInt(unicode, 16);
				builder.append((char) value);
			} catch(NumberFormatException ignored) {
				return 0;
			}
			return i + 4;
		}
		return 0;
	}

	private static int escapeStandard(String input, int cursor, StringBuilder builder) {
		if(input.charAt(cursor) != '\\')
			return 0;
		if(cursor + 1 >= input.length())
			return 0;
		char next = input.charAt(cursor + 1);
		switch(next) {
			case 'n':
				builder.append('\n');
				return 2;
			case 'r':
				builder.append('\r');
				return 2;
			case 't':
				builder.append('\t');
				return 2;
			default:
				return 0;
		}
	}
}
