package me.coley.recaf.util;

import org.reactfx.util.TriFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Escape code replacement utility.
 *
 * @author xxDark
 */
public final class EscapeUtil {
	private static final Map<String, String> WHITESPACE_TO_ESCAPE = new HashMap<>();
	private static final Map<String, String> ESCAPE_TO_WHITESPACE = new HashMap<>();
	private static final char TERMINATOR = '\0';

	private EscapeUtil() {}

	/**
	 * Replaces any unicode-whitespace or common escapable sequence with an escaped sequence.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String without escaped characters.
	 */
	public static String escape(String input) {
		return visit(escapeCommon(input), EscapeUtil::computeUnescapeUnicode);
	}

	/**
	 * Replaces any common escapable sequence with an escaped sequence.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String without common escaped characters.
	 */
	public static String escapeCommon(String input) {
		return visit(input, EscapeUtil::computeUnescapeStandard);
	}

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
		return visit(input, EscapeUtil::computeEscapeUnicode);
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
		return visit(input, EscapeUtil::computeEscapeStandard);
	}

	private static String visit(String input, TriFunction<String, Integer, StringBuilder, Integer> consumer) {
		int len = input.length();
		int cursor = 0;
		StringBuilder builder = new StringBuilder(len);
		while(cursor < len) {
			int consumed = consumer.apply(input, cursor, builder);
			if (consumed == 0) {
				// Nothing consumed, not an escaped character
				char c1 = input.charAt(cursor++);
				builder.append(c1);
				// Does additional character need to be appended?
				if (Character.isHighSurrogate(c1) && cursor < len) {
					char c2 = input.charAt(cursor);
					if (Character.isLowSurrogate(c2)) {
						builder.append(c2);
						cursor += 1;
					}
				}
			} else {
				// Shift cursor by amount consumed
				for (int pt = 0; pt < consumed; ++pt) {
					cursor += Character.charCount(Character.codePointAt(input, cursor));
				}
			}
		}
		return builder.toString();
	}

	private static int computeUnescapeUnicode(String input, int cursor, StringBuilder builder) {
		// Bounds check
		if (cursor >= input.length()) {
			return 0;
		}
		// Check if next character finishes an unescape value, 1 if so, 0 if not.
		String current = String.valueOf(input.charAt(cursor));
		String escaped = WHITESPACE_TO_ESCAPE.get(current);
		if (escaped != null) {
			builder.append(escaped);
			return 1;
		}
		// No replacement
		return 0;
	}

	private static int computeUnescapeStandard(String input, int cursor, StringBuilder builder) {
		// Bounds check
		if (cursor >= input.length()) {
			return 0;
		}
		// Check if next character finishes an unescape value, 1 if so, 0 if not.
		char current = input.charAt(cursor);
		switch(current) {
			case '\n':
				builder.append("\\n");
				return 1;
			case '\r':
				builder.append("\\r");
				return 1;
			case '\t':
				builder.append("\\t");
				return 1;
			case '\\':
				builder.append("\\\\");
				return 1;
			default:
				return 0;
		}
	}

	private static int computeEscapeUnicode(String input, int cursor, StringBuilder builder) {
		// Bounds check
		if (cursor + 1 >= input.length()) {
			return 0;
		}

		// Check for double backslash in prefix "\\\\u" in "\\\\uXXXX"
		boolean initialEscape = input.charAt(cursor) == '\\' && input.charAt(cursor + 1) == '\\';

		// Check prefix "\\u" in "\\uXXXX"
		if (!initialEscape) {
			if (input.charAt(cursor) != '\\' || input.charAt(cursor + 1) != 'u') {
				return 0;
			}
		}

		// Compute escape size, initial is 2 for the "\\u"
		int len = 2;
		// Combined:
		// - Bounds check
		// - Case check for "\\uuXXXX" where 'u' is repeated
		while(cursor + len < input.length() && input.charAt(cursor + len) == 'u') {
			len++;
		}
		// Combined:
		// - Bounds check
		// - Case check for "\\u+XXXX" format
		if (cursor + len < input.length() && input.charAt(cursor + len) == '+') {
			len += 1;
		}
		// Bounds check, then fetch hex value and store in builder, then return total consumed length
		if (cursor + len + 4 <= input.length()) {
			String substring = input.substring(cursor, cursor + len + 4);

			if (initialEscape) {
				builder.append(substring);
				return len + 4;
			}

			String unicode = input.substring(cursor + len, cursor + len + 4);
			try {
				int value = Integer.parseInt(unicode, 16);
				builder.append(value != TERMINATOR ? (char) value : substring);
			} catch(NumberFormatException ignored) {
				return 0;
			}
			return len + 4;
		}
		return 0;
	}

	private static int computeEscapeStandard(String input, int cursor, StringBuilder builder) {
		// Bounds check
		if (cursor + 1 >= input.length()) {
			return 0;
		}
		// Check prefix '\' in "\X"
		if (input.charAt(cursor) != '\\') {
			return 0;
		}
		// Check if next character finishes the escape pattern, 2 if so, 0 if not.
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
			case '\\':
				builder.append('\\');
				return 2;
			default:
				return 0;
		}
	}

	static void addWhitespace(String unescape, String escape) {
		// Mapping between whitespace unicode value and character
		WHITESPACE_TO_ESCAPE.put(unescape, escape);
		ESCAPE_TO_WHITESPACE.put(escape, unescape);
	}

	static {
		//Unicode whitespaces
		for (int i = 0; i < 0x20; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x7F; i < 0xA0; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x6E5; i < 0x6E6; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x17B4; i < 0x17B5; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x180B; i < 0x180E; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x2000; i < 0x200F; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x2028; i < 0x202F; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		for (int i = 0x205F; i < 0x206F; i++) {
			addWhitespace(String.valueOf(Character.toChars(i)), "\\u" + String.format("%04X", i));
		}
		addWhitespace(String.valueOf(Character.toChars('\u3164')), "\\u" + String.format("%04X", (int) '\u3164'));
		addWhitespace(String.valueOf(Character.toChars('\u318F')), "\\u" + String.format("%04X", (int) '\u318F'));
	}
}
