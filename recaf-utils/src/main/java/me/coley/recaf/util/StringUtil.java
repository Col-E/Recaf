package me.coley.recaf.util;

import com.carrotsearch.hppc.CharIntHashMap;
import com.carrotsearch.hppc.CharIntMap;
import com.carrotsearch.hppc.cursors.CharIntCursor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Various utilities for {@link String} manipulation.
 *
 * @author Matt Coley
 */
public class StringUtil {
	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 */
	public static String[] splitNewline(String input) {
		return input.split("\n\r|\r\n|\n");
	}

	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 * Empty lines <i>(Containing only the newline split)</i> are omitted.
	 */
	public static String[] splitNewlineSkipEmpty(String input) {
		String[] split = input.split("[\r\n]+");
		// If the first line of the file is a newline split will still have
		// one blank entry at the start.
		if (split[0].isEmpty())
			return Arrays.copyOfRange(split, 1, split.length);
		return split;
	}

	/**
	 * @param defaultText
	 * 		Text to return.
	 * @param fallback
	 * 		Fallback to use if the default text is blank.
	 *
	 * @return Text or fallback if text is empty.
	 */
	public static String withEmptyFallback(String defaultText, String fallback) {
		if (defaultText == null || defaultText.trim().isBlank())
			return fallback;
		return defaultText;
	}

	/**
	 * @param text
	 * 		Text to cut.
	 * @param start
	 * 		Position from start forward to cut from.
	 * @param end
	 * 		Position from end to cut off.
	 *
	 * @return Substring text in range.
	 */
	public static String substringRelative(String text, int start, int end) {
		return text.substring(start, text.length() - end);
	}

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
	 * @param text
	 * 		Input text.
	 * @param max
	 * 		Max length of text.
	 *
	 * @return Text cut off to max length.
	 */
	public static String limit(String text, int max) {
		return limit(text, null, max);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoffPattern
	 * 		Text to append to text that gets limited.
	 * @param max
	 * 		Max length of text.
	 *
	 * @return Text cut off to max length.
	 */
	public static String limit(String text, String cutoffPattern, int max) {
		if (text.length() > max) {
			String s = text.substring(0, max);
			if (cutoffPattern != null)
				s += cutoffPattern;
			return s;
		}
		return text;
	}

	/**
	 * @param name
	 * 		Text to lowercase the first letter of.
	 *
	 * @return Text with first letter lower-cased.
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
	 * @param name
	 * 		Text to uppercase the first letter of.
	 *
	 * @return Text with first letter upper-cased.
	 */
	public static String uppercaseFirstChar(String name) {
		int len = name.length();
		if (len == 1)
			return name.toUpperCase();
		else if (len > 1)
			return name.substring(0, 1).toUpperCase() + name.substring(1);
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
		StringBuilder sb = new StringBuilder(text.length() * times);
		for (int i = 0; i < times; i++)
			sb.append(text);
		return sb.toString();
	}

	/**
	 * @param args
	 * 		Input arguments.
	 *
	 * @return {@code true} when any of the arguments is {@code null} or an empty string.
	 */
	public static boolean isAnyNullOrEmpty(String... args) {
		for (String arg : args)
			if (StringUtil.isNullOrEmpty(arg))
				return true;
		return false;
	}

	/**
	 * @param text
	 * 		Text to check.
	 *
	 * @return {@code true} when the text is either {@code null} or an empty string.
	 */
	public static boolean isNullOrEmpty(String text) {
		return text == null || text.isEmpty();
	}

	/**
	 * @param text
	 * 		Input text.
	 *
	 * @return Entropy of the characters in the text.
	 */
	public static double getEntropy(String text) {
		CharIntMap charCountMap = new CharIntHashMap(64);
		for (char c : text.toCharArray())
			charCountMap.addTo(c, 1);
		int length = text.length();
		final double[] entropy = {0.0};
		charCountMap.forEach((Consumer<CharIntCursor>) cursor -> {
			double freq = ((double) cursor.value) / length;
			entropy[0] -= freq * (Math.log(freq) / Math.log(2));
		});
		return entropy[0];
	}

	/**
	 * @param t
	 * 		Throwable error.
	 *
	 * @return Stacktrace as string.
	 */
	public static String traceToString(Throwable t) {
		StringWriter trace = new StringWriter();
		t.printStackTrace(new PrintWriter(trace));
		return trace.toString();
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
		if (data.length == 0) {
			return false;
		}
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int length = data.length;
		int entropy = 0;
		int bufferSize = (int) (length * 0.01D);
		if (bufferSize > 4096)
			bufferSize = 4096;
		else if (bufferSize == 0)
			bufferSize = length; // Small file, set to length
		char[] charArray = new char[bufferSize];
		CharBuffer charBuf = CharBuffer.wrap(charArray);
		while (true) {
			if (!buffer.hasRemaining()) {
				return true;
			}
			CoderResult result = decoder.decode(buffer, charBuf, true);
			if (result.isUnderflow())
				decoder.flush(charBuf);
			entropy = calculateNonText(charArray, entropy, length, charBuf.position());
			if (entropy == -1) {
				return false;
			}
			charBuf.rewind();
		}
	}

	private static int calculateNonText(char[] charBuf, int entropy, int max, int length) {
		int index = 0;
		while (length-- != 0) {
			char c = charBuf[index++];
			int codePoint;
			merge:
			{
				if (Character.isHighSurrogate(c)) {
					if (length != 0) {
						char c2 = charBuf[index];
						if (Character.isLowSurrogate(c2)) {
							index++;
							length--;
							codePoint = Character.toCodePoint(c, c2);
							break merge;
						}
					}
				}
				codePoint = c;
			}
			if (codePoint <= 31) {
				switch (codePoint) {
					case 9:
					case 10:
					case 13:
						continue;
					default:
						entropy++;
				}
			} else if (codePoint >= 57344 && codePoint <= 63743) {
				entropy++;
			} else if (codePoint >= 65520 && codePoint <= 65535) {
				entropy++;
			} else if (codePoint >= 983040 && codePoint <= 1048573) {
				entropy++;
			} else if (codePoint >= 1048576 && codePoint <= 1114109) {
				entropy++;
			}
		}
		if (entropy / (double) max > 0.01D)
			return -1;
		return entropy;
	}
}
