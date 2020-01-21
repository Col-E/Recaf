package me.coley.recaf.util;

/**
 * Utils to work with Unicode text.
 *
 * @author xxDark
 */
public final class UnicodeUtil {

	private UnicodeUtil() { }

	/**
	 * Replaces Unicode characters.
	 *
	 * @param input input text
	 * @return string with escaped characters.
	 */
	public static String unescape(String input) {
		int len = input.length();
		int cursor = 0;
		StringBuilder builder = new StringBuilder(len);
		while (cursor < len) {
			int consumed = escape(input, cursor, builder);
			if (consumed == 0) {
				char c1 = input.charAt(cursor++);
				builder.append(c1);
				if (Character.isHighSurrogate(c1) && cursor < len) {
					char c2 = input.charAt(cursor);
					if (Character.isLowSurrogate(c2)) {
						builder.append(c2);
						cursor += 1;
					}
				}
			} else {
				for (int pt = 0; pt < consumed; ++pt) {
					cursor += Character.charCount(Character.codePointAt(input, cursor));
				}
			}
		}
		return builder.toString();
	}

	private static int escape(String input, int cursor, StringBuilder builder) {
		if (input.charAt(cursor) != '\\') return 0;
		if (cursor + 1 >= input.length()) return 0;
		if (input.charAt(cursor + 1) != 'u') return 0;
		int i;
		for (i = 2; cursor + i < input.length() && input.charAt(cursor + i) == 'u'; i++) ;

		if (cursor + i < input.length() && input.charAt(cursor + i) == '+') {
			i += 1;
		}

		if (cursor + i + 4 <= input.length()) {
			String unicode = input.substring(cursor + i, cursor + i + 4);
			try {
				int value = Integer.parseInt(unicode, 16);
				builder.append((char) value);
			} catch (NumberFormatException ignored) {
				return 0;
			}
			return i + 4;
		}
		return 0;
	}
}
