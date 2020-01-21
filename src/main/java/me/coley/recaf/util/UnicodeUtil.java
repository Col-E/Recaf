package me.coley.recaf.util;

import com.github.javaparser.utils.StringEscapeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils to work with Unicode text.
 *
 * @author xxDark
 */
public final class UnicodeUtil {
	private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("^(?!\\u00A7[0-9A-FK-OR])[^\\x00-\\x7F]+");

	private UnicodeUtil() { }

	/**
	 * Replaces Unicode characters.
	 *
	 * @param input input text
	 * @return string with escaped characters.
	 */
	public static String unescape(String input) {
		StringBuffer buffer = new StringBuffer(input.length());
		Matcher matcher = PATTERN_CONTROL_CODE.matcher(input);
		while (matcher.find()) {
			matcher.appendReplacement(buffer, StringEscapeUtils.unescapeJava(matcher.group(1)));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
