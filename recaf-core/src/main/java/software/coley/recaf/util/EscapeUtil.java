package software.coley.recaf.util;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.objects.Object2CharMap;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Escape code replacement utility.
 *
 * @author xDark
 */
public final class EscapeUtil {
	private static final Char2ObjectMap<String> WHITESPACE_TO_ESCAPE = new Char2ObjectOpenHashMap<>();
	private static final Object2CharMap<String> ESCAPE_TO_WHITESPACE = new Object2CharOpenHashMap<>(); // TODO: Shouldn't we use this?
	private static final Set<String> WHITESPACE_STRINGS = new TreeSet<>();
	private static final BitSet WHITESPACE_LOOKUP = new BitSet(Character.MAX_VALUE + 1);
	public static final char TERMINATOR = '\0';
	public static final String ESCAPED_SPACE = "\\u0020";
	public static final String ESCAPED_TAB = "\\u0009";
	public static final String ESCAPED_NEWLINE = "\\u000A";
	public static final String ESCAPED_RETURN = "\\u000D";
	public static final String ESCAPED_DOUBLE_QUOTE = "\\u0022";
	public static final String ESCAPED_BACKWARDS_SLASH = "\\\\";

	private EscapeUtil() {}

	/**
	 * @param text
	 * 		Text to check.
	 *
	 * @return {@code true} when text contains any whitespace characters.
	 */
	public static boolean containsWhitespace(@Nonnull String text) {
		int len = text.length();
		for (int i = 0; i < len; i++)
			if (WHITESPACE_LOOKUP.get(text.charAt(i)))
				return true;
		return false;
	}

	/**
	 * @param c
	 * 		Character to check.
	 *
	 * @return {@code true} if it represents a whitespace character.
	 */
	public static boolean isWhitespaceChar(char c) {
		return WHITESPACE_LOOKUP.get(c);
	}

	/**
	 * @return Set of strings representing various whitespaces.
	 *
	 * @see #getWhitespaceChars() Alternative offering the values as {@code char}
	 */
	@Nonnull
	public static Set<String> getWhitespaceStrings() {
		return WHITESPACE_STRINGS;
	}

	/**
	 * @return Set of chars representing various whitespaces.
	 */
	@Nonnull
	public static CharSet getWhitespaceChars() {
		return WHITESPACE_TO_ESCAPE.keySet();
	}

	/**
	 * Replaces any common escapable sequence with an escaped sequence.
	 * <br>
	 * For example: {@code \n}, {@code \r}, {@code \t}, {@code \}, and {@code "}
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String without common escaped characters.
	 */
	public static String escapeStandard(@Nullable String input) {
		return visit(input, EscapeUtil::computeUnescapeStandard);
	}

	/**
	 * Replaces any unicode-whitespace or common escapable sequence with an escaped sequence.
	 * <br>
	 * Combines both standard sequences and less common unicode whitespaces.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String without escaped characters.
	 */
	public static String escapeStandardAndUnicodeWhitespace(@Nullable String input) {
		return visit(escapeStandard(input), EscapeUtil::computeUnescapeUnicode);
	}

	/**
	 * Replaces any standard/unicode-whitespace escape targets with unicode escapes.
	 * <br>
	 * Combines both standard sequences and less common unicode whitespaces.
	 * <br>
	 * Even standard escape targets are escaped using unicode escapes.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String without escaped characters.
	 */
	public static String escapeStandardAndUnicodeWhitespaceAlt(@Nullable String input) {
		return visit(input, EscapeUtil::computeUnescapeUnicodeJasm);
	}

	/**
	 * Replaces any escape code with its literal value.
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescapeStandardAndUnicodeWhitespace(@Nullable String input) {
		return unescapeStandard(unescapeUnicode(input));
	}

	/**
	 * Replaces escaped unicode with actual unicode.
	 * <br>
	 * For example: {@code \\u0048} --> {@code H}
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescapeUnicode(@Nullable String input) {
		return visit(input, EscapeUtil::computeEscapeUnicode);
	}

	/**
	 * Replaces standard escape codes with literal values.
	 * <br>
	 * For example: {@code \n}, {@code \r}, {@code \t}, {@code \}, and {@code "}
	 *
	 * @param input
	 * 		Input text.
	 *
	 * @return String with escaped characters.
	 */
	public static String unescapeStandard(@Nullable String input) {
		return visit(input, EscapeUtil::computeEscapeStandard);
	}

	private static String visit(@Nullable String input, @Nonnull Visitor consumer) {
		if (input == null)
			return null;
		int len = input.length();
		int cursor = 0;
		StringBuilder builder = new StringBuilder(len);
		while (cursor < len) {
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

		// Check if next character finishes an unescaped value, 1 if so, 0 if not.
		char current = input.charAt(cursor);
		String escaped = WHITESPACE_TO_ESCAPE.get(current);
		if (escaped != null) {
			builder.append(escaped);
			return 1;
		}

		// No replacement
		return 0;
	}

	private static int computeUnescapeUnicodeJasm(String input, int cursor, StringBuilder builder) {
		// Bounds check
		if (cursor >= input.length())
			return 0;

		// Check if character is a special char
		char current = input.charAt(cursor);
		String escaped = switch (current) {
			case ' ' -> ESCAPED_SPACE;
			case '\t' -> ESCAPED_TAB;
			case '\n' -> ESCAPED_NEWLINE;
			case '\r' -> ESCAPED_RETURN;
			case '\"' -> ESCAPED_DOUBLE_QUOTE;
			case '\\' -> ESCAPED_BACKWARDS_SLASH;
			default -> WHITESPACE_TO_ESCAPE.get(current);
		};

		// Check if next character finishes an unescaped value, 1 if so, 0 if not.
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

		// Check if next character finishes an unescaped value, 1 if so, 0 if not.
		char current = input.charAt(cursor);
		switch (current) {
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
			case '"':
				builder.append("\\\"");
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
		char c1 = input.charAt(cursor);
		char c2 = input.charAt(cursor + 1);
		boolean initialEscape = c1 == '\\' && c2 == '\\';

		// Check prefix "\\u" in "\\uXXXX"
		if (!initialEscape && (c1 != '\\' || c2 != 'u')) {
			return 0;
		}

		// Compute escape size, initial is 2 for the "\\u"
		final int initialLen = 2;
		int len = initialLen;

		// Combined:
		// - Bounds check
		// - Case check for "\\uuXXXX" where 'u' is repeated
		while (cursor + len < input.length() && input.charAt(cursor + len) == 'u') {
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
			String existing = input.substring(cursor, cursor + len + 4);
			if (initialEscape) {
				// Keep the '\\uXXXX' format.
				builder.append(existing);
			} else {
				String unicode = input.substring(cursor + len, cursor + len + 4);
				try {
					int value = Integer.parseInt(unicode, 16);
					builder.append(value != TERMINATOR ? (char) value : existing);
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
			return len + 4;
		}

		// If we didn't see any recognized pattern, just escape "\\\\" into "\\"
		if (initialEscape && len == initialLen) {
			return 1;
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
		switch (next) {
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
			case '"':
				builder.append('"');
				return 2;
			default:
				return 0;
		}
	}

	private interface Visitor {
		int apply(String input, int cursor, StringBuilder builder);
	}

	static void addWhitespace(char unescape, String escape) {
		// Mapping between whitespace unicode value and character
		WHITESPACE_STRINGS.add(String.valueOf(unescape));
		WHITESPACE_TO_ESCAPE.put(unescape, escape);
		ESCAPE_TO_WHITESPACE.put(escape, unescape);
		WHITESPACE_LOOKUP.set(unescape);
	}

	static {
		//Unicode whitespaces or visually empty characters (like null terminators and such)
		for (char i = 0; i < 0x20; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x7F; i < 0xA0; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x6E5; i < 0x6E6; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x17B4; i < 0x17B5; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x180B; i < 0x180E; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x2000; i < 0x200F; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x2028; i < 0x202F; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x205F; i < 0x206F; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0x2400; i < 0x243F; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0xE000; i < 0xF8FF; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0xFE00; i < 0xFE0F; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0xFE1A; i < 0xFE20; i++) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		for (char i = 0xFFFF; i >= 0xFFF0; i--) {
			addWhitespace(i, "\\u" + String.format("%04X", (int) i));
		}
		addWhitespace((('ㅤ')), "\\u" + String.format("%04X", (int) 'ㅤ'));
		addWhitespace((('\u1680')), "\\u" + String.format("%04X", (int) '\u1680'));
		addWhitespace((('\u2800')), "\\u" + String.format("%04X", (int) '\u2800'));
		addWhitespace((('\u3000')), "\\u" + String.format("%04X", (int) '\u3000'));
		addWhitespace((('\u318F')), "\\u" + String.format("%04X", (int) '\u318F'));
	}
}
