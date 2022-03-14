package me.coley.recaf.util;

import java.nio.charset.StandardCharsets;

/**
 * Various utilities for {@link String} manipulation.
 *
 * @author Matt Coley
 */
public class StringUtil {
	private static final char NULL_TERMINATOR = '\0';

	/**
	 * @param text
	 * 		Text to check.
	 *
	 * @return {@code true} when the given text represents a decimal number.
	 */
	public static boolean isDecimal(String text) {
		try {
			Double.parseDouble(text);
			return false;
		} catch (NumberFormatException ex) {
			return false;
		}
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

	/**
	 * @param name
	 * 		Path name to shorten.
	 *
	 * @return Shortened name.
	 */
	public static String shortenPath(String name) {
		int separatorIndex = name.lastIndexOf('/');
		if (separatorIndex > 0)
			name = name.substring(separatorIndex + 1);
		return name;
	}

	/**
	 * @param name
	 * 		Text to lowercase the first letter of.
	 *
	 * @return Text with first letter lowered.
	 */
	public static String lowercaseFirstChar(String name) {
		int len = name.length();
		if (len == 1)
			return name.toLowerCase();
		else if (len > 1)
			return name.substring(0, 1).toLowerCase() + name.substring(1);
		else
			return name;
	}

	/**
	 * @param len
	 * 		Target string length.
	 * @param pattern
	 * 		Pattern to fill with.
	 * @param string
	 * 		Initial string.
	 *
	 * @return String with pattern filling up to the desired length on the left.
	 */
	public static String fillLeft(int len, String pattern, String string) {
		StringBuilder sb = new StringBuilder(string);
		while (sb.length() < len) {
			sb.insert(0, pattern);
		}
		return sb.toString();
	}

	/**
	 * @param len
	 * 		Target string length.
	 * @param pattern
	 * 		Pattern to fill with.
	 * @param string
	 * 		Initial string.
	 *
	 * @return String with pattern filling up to the desired length on the right.
	 */
	public static String fillRight(int len, String pattern, String string) {
		StringBuilder sb = new StringBuilder(string);
		while (sb.length() < len) {
			sb.append(pattern);
		}
		return sb.toString();
	}

	/**
	 * @param text
	 * 		Text to repeat.
	 * @param times
	 * 		Number of repetitions.
	 *
	 * @return Repeated text.
	 */
	public static String repeat(String text, int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++)
			sb.append(text);
		return sb.toString();
	}

	/**
	 * Wrapper for {@link Integer#toHexString(int)} that pads zeros when needed.
	 *
	 * @param value
	 * 		Value to print.
	 *
	 * @return Hex printed value.
	 */
	public static String toHexString(int value) {
		return StringUtil.fillLeft(2, "0", Integer.toHexString(value & 0xff));
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
	 * @param data
	 * 		Some data to check.
	 *
	 * @return {@code true} when it contains only text.
	 */
	public static boolean isText(byte[] data) {
		String string = new String(data, StandardCharsets.UTF_8);
		int nonText = 0;
		for (int i = 0; i < string.length(); i++) {
			int codePoint = string.codePointAt(i);
			int type = Character.getType(codePoint);
			int skip = Character.charCount(codePoint) - 1;
			i += skip;
			switch (type) {
				case Character.CONTROL:
					if (codePoint == 13 || codePoint == 10 || codePoint == 9)
						break; // Don't treat "\r", "\n", "\t" as standard control chars
					nonText++;
					break;
				case Character.UNASSIGNED:
					if (isEmoji(codePoint))
						break; // Plenty of emoji land in this category
					nonText++;
					break;
				default:
					break;
			}
		}
		double nonTextPercent = (nonText / (double) data.length);
		return nonTextPercent <= 0.01;
	}

	/**
	 * Checks if a character code-point matches an emoji range.
	 *
	 * @param codePoint
	 *        {@link Character#codePointAt(char[], int)} from a {@code char[]}.
	 *
	 * @return {@code true} for an emoji.
	 */
	public static boolean isEmoji(int codePoint) {
		// From: http://www.unicode.org/emoji/charts/full-emoji-list.html
		// Scraped with:
		//   var codes = document.getElementsByClassName("code")
		//   var i = 0
		//   for (i = 0; i < codes.length; i++) {
		//      console.log(codes[i].innerText)
		//   }
		if (codePoint == 0x23) return true;
		if (codePoint == 0x2A) return true;
		if (codePoint >= 0x30 && codePoint <= 0x39) return true;
		if (codePoint == 0xA9) return true;
		if (codePoint == 0xAE) return true;
		if (codePoint == 0x200D) return true;
		if (codePoint == 0x203C) return true;
		if (codePoint == 0x2049) return true;
		if (codePoint == 0x20E3) return true;
		if (codePoint == 0x2122) return true;
		if (codePoint == 0x2139) return true;
		if (codePoint >= 0x2194 && codePoint <= 0x2199) return true;
		if (codePoint >= 0x21A9 && codePoint <= 0x21AA) return true;
		if (codePoint >= 0x231A && codePoint <= 0x231B) return true;
		if (codePoint == 0x2328) return true;
		if (codePoint == 0x23CF) return true;
		if (codePoint >= 0x23E9 && codePoint <= 0x23F3) return true;
		if (codePoint >= 0x23F8 && codePoint <= 0x23FA) return true;
		if (codePoint == 0x24C2) return true;
		if (codePoint >= 0x25AA && codePoint <= 0x25AB) return true;
		if (codePoint == 0x25B6) return true;
		if (codePoint == 0x25C0) return true;
		if (codePoint >= 0x25FB && codePoint <= 0x25FE) return true;
		if (codePoint >= 0x2600 && codePoint <= 0x2604) return true;
		if (codePoint == 0x260E) return true;
		if (codePoint == 0x2611) return true;
		if (codePoint >= 0x2614 && codePoint <= 0x2615) return true;
		if (codePoint == 0x2618) return true;
		if (codePoint == 0x261D) return true;
		if (codePoint == 0x2620) return true;
		if (codePoint >= 0x2622 && codePoint <= 0x2623) return true;
		if (codePoint == 0x2626) return true;
		if (codePoint == 0x262A) return true;
		if (codePoint >= 0x262E && codePoint <= 0x262F) return true;
		if (codePoint >= 0x2638 && codePoint <= 0x263A) return true;
		if (codePoint == 0x2640) return true;
		if (codePoint == 0x2642) return true;
		if (codePoint >= 0x2648 && codePoint <= 0x2653) return true;
		if (codePoint >= 0x265F && codePoint <= 0x2660) return true;
		if (codePoint == 0x2663) return true;
		if (codePoint >= 0x2665 && codePoint <= 0x2666) return true;
		if (codePoint == 0x2668) return true;
		if (codePoint == 0x267B) return true;
		if (codePoint >= 0x267E && codePoint <= 0x267F) return true;
		if (codePoint >= 0x2692 && codePoint <= 0x2697) return true;
		if (codePoint == 0x2699) return true;
		if (codePoint >= 0x269B && codePoint <= 0x269C) return true;
		if (codePoint >= 0x26A0 && codePoint <= 0x26A1) return true;
		if (codePoint == 0x26A7) return true;
		if (codePoint >= 0x26AA && codePoint <= 0x26AB) return true;
		if (codePoint >= 0x26B0 && codePoint <= 0x26B1) return true;
		if (codePoint >= 0x26BD && codePoint <= 0x26BE) return true;
		if (codePoint >= 0x26C4 && codePoint <= 0x26C5) return true;
		if (codePoint == 0x26C8) return true;
		if (codePoint >= 0x26CE && codePoint <= 0x26CF) return true;
		if (codePoint == 0x26D1) return true;
		if (codePoint >= 0x26D3 && codePoint <= 0x26D4) return true;
		if (codePoint >= 0x26E9 && codePoint <= 0x26EA) return true;
		if (codePoint >= 0x26F0 && codePoint <= 0x26F5) return true;
		if (codePoint >= 0x26F7 && codePoint <= 0x26FA) return true;
		if (codePoint == 0x26FD) return true;
		if (codePoint == 0x2702) return true;
		if (codePoint == 0x2705) return true;
		if (codePoint >= 0x2708 && codePoint <= 0x270D) return true;
		if (codePoint == 0x270F) return true;
		if (codePoint == 0x2712) return true;
		if (codePoint == 0x2714) return true;
		if (codePoint == 0x2716) return true;
		if (codePoint == 0x271D) return true;
		if (codePoint == 0x2721) return true;
		if (codePoint == 0x2728) return true;
		if (codePoint >= 0x2733 && codePoint <= 0x2734) return true;
		if (codePoint == 0x2744) return true;
		if (codePoint == 0x2747) return true;
		if (codePoint == 0x274C) return true;
		if (codePoint == 0x274E) return true;
		if (codePoint >= 0x2753 && codePoint <= 0x2755) return true;
		if (codePoint == 0x2757) return true;
		if (codePoint >= 0x2763 && codePoint <= 0x2764) return true;
		if (codePoint >= 0x2795 && codePoint <= 0x2797) return true;
		if (codePoint == 0x27A1) return true;
		if (codePoint == 0x27B0) return true;
		if (codePoint == 0x27BF) return true;
		if (codePoint >= 0x2934 && codePoint <= 0x2935) return true;
		if (codePoint >= 0x2B05 && codePoint <= 0x2B07) return true;
		if (codePoint >= 0x2B1B && codePoint <= 0x2B1C) return true;
		if (codePoint == 0x2B50) return true;
		if (codePoint == 0x2B55) return true;
		if (codePoint == 0x3030) return true;
		if (codePoint == 0x303D) return true;
		if (codePoint == 0x3297) return true;
		if (codePoint == 0x3299) return true;
		if (codePoint == 0xFE0F) return true;
		if (codePoint == 0x1F004) return true;
		if (codePoint == 0x1F0CF) return true;
		if (codePoint >= 0x1F170 && codePoint <= 0x1F171) return true;
		if (codePoint >= 0x1F17E && codePoint <= 0x1F17F) return true;
		if (codePoint == 0x1F18E) return true;
		if (codePoint >= 0x1F191 && codePoint <= 0x1F19A) return true;
		if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) return true;
		if (codePoint >= 0x1F201 && codePoint <= 0x1F202) return true;
		if (codePoint == 0x1F21A) return true;
		if (codePoint == 0x1F22F) return true;
		if (codePoint >= 0x1F232 && codePoint <= 0x1F23A) return true;
		if (codePoint >= 0x1F250 && codePoint <= 0x1F251) return true;
		if (codePoint >= 0x1F300 && codePoint <= 0x1F321) return true;
		if (codePoint >= 0x1F324 && codePoint <= 0x1F393) return true;
		if (codePoint >= 0x1F396 && codePoint <= 0x1F397) return true;
		if (codePoint >= 0x1F399 && codePoint <= 0x1F39B) return true;
		if (codePoint >= 0x1F39E && codePoint <= 0x1F3F0) return true;
		if (codePoint >= 0x1F3F3 && codePoint <= 0x1F3F5) return true;
		if (codePoint >= 0x1F3F7 && codePoint <= 0x1F3FA) return true;
		if (codePoint >= 0x1F400 && codePoint <= 0x1F4FD) return true;
		if (codePoint >= 0x1F4FF && codePoint <= 0x1F53D) return true;
		if (codePoint >= 0x1F549 && codePoint <= 0x1F54E) return true;
		if (codePoint >= 0x1F550 && codePoint <= 0x1F567) return true;
		if (codePoint >= 0x1F56F && codePoint <= 0x1F570) return true;
		if (codePoint >= 0x1F573 && codePoint <= 0x1F57A) return true;
		if (codePoint == 0x1F587) return true;
		if (codePoint >= 0x1F58A && codePoint <= 0x1F58D) return true;
		if (codePoint == 0x1F590) return true;
		if (codePoint >= 0x1F595 && codePoint <= 0x1F596) return true;
		if (codePoint >= 0x1F5A4 && codePoint <= 0x1F5A5) return true;
		if (codePoint == 0x1F5A8) return true;
		if (codePoint >= 0x1F5B1 && codePoint <= 0x1F5B2) return true;
		if (codePoint == 0x1F5BC) return true;
		if (codePoint >= 0x1F5C2 && codePoint <= 0x1F5C4) return true;
		if (codePoint >= 0x1F5D1 && codePoint <= 0x1F5D3) return true;
		if (codePoint >= 0x1F5DC && codePoint <= 0x1F5DE) return true;
		if (codePoint == 0x1F5E1) return true;
		if (codePoint == 0x1F5E3) return true;
		if (codePoint == 0x1F5E8) return true;
		if (codePoint == 0x1F5EF) return true;
		if (codePoint == 0x1F5F3) return true;
		if (codePoint >= 0x1F5FA && codePoint <= 0x1F64F) return true;
		if (codePoint >= 0x1F680 && codePoint <= 0x1F6C5) return true;
		if (codePoint >= 0x1F6CB && codePoint <= 0x1F6D2) return true;
		if (codePoint >= 0x1F6D5 && codePoint <= 0x1F6D7) return true;
		if (codePoint >= 0x1F6DD && codePoint <= 0x1F6E5) return true;
		if (codePoint == 0x1F6E9) return true;
		if (codePoint >= 0x1F6EB && codePoint <= 0x1F6EC) return true;
		if (codePoint == 0x1F6F0) return true;
		if (codePoint >= 0x1F6F3 && codePoint <= 0x1F6FC) return true;
		if (codePoint >= 0x1F7E0 && codePoint <= 0x1F7EB) return true;
		if (codePoint == 0x1F7F0) return true;
		if (codePoint >= 0x1F90C && codePoint <= 0x1F93A) return true;
		if (codePoint >= 0x1F93C && codePoint <= 0x1F945) return true;
		if (codePoint >= 0x1F947 && codePoint <= 0x1F9FF) return true;
		if (codePoint >= 0x1FA70 && codePoint <= 0x1FA74) return true;
		if (codePoint >= 0x1FA78 && codePoint <= 0x1FA7C) return true;
		if (codePoint >= 0x1FA80 && codePoint <= 0x1FA86) return true;
		if (codePoint >= 0x1FA90 && codePoint <= 0x1FAAC) return true;
		if (codePoint >= 0x1FAB0 && codePoint <= 0x1FABA) return true;
		if (codePoint >= 0x1FAC0 && codePoint <= 0x1FAC5) return true;
		if (codePoint >= 0x1FAD0 && codePoint <= 0x1FAD9) return true;
		if (codePoint >= 0x1FAE0 && codePoint <= 0x1FAE7) return true;
		if (codePoint >= 0x1FAF0 && codePoint <= 0x1FAF6) return true;
		if (codePoint >= 0xE0062 && codePoint <= 0xE0063) return true;
		if (codePoint == 0xE0065) return true;
		if (codePoint == 0xE0067) return true;
		if (codePoint == 0xE006C) return true;
		if (codePoint == 0xE006E) return true;
		if (codePoint >= 0xE0073 && codePoint <= 0xE0074) return true;
		if (codePoint == 0xE0077) return true;
		if (codePoint == 0xE007F) return true;
		return false;
	}
}
