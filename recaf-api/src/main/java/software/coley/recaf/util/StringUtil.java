package software.coley.recaf.util;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

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
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Sequence to match up to.
	 *
	 * @return Input text, up until the first cutoff sequence.
	 */
	public static String cutOffAtFirst(@Nonnull String text, @Nonnull String cutoff) {
		int i = text.indexOf(cutoff);
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param insertIndex
	 * 		Position to insert into.
	 * @param insertion
	 * 		Text to insert.
	 *
	 * @return Modified text.
	 */
	public static String insert(@Nonnull String text, int insertIndex, @Nullable String insertion) {
		if (insertion == null || insertion.isEmpty() || insertIndex < 0 || insertIndex >= text.length()) return text;
		String pre = text.substring(0, insertIndex);
		String post = text.substring(insertIndex);
		return pre + insertion + post;
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param removeIndex
	 * 		Position to remove at.
	 * @param removeLength
	 * 		Length of removal.
	 *
	 * @return Modified text.
	 */
	public static String remove(@Nonnull String text, int removeIndex, int removeLength) {
		if (removeIndex < 0 || removeIndex >= text.length()) return text;
		String pre = text.substring(0, removeIndex);
		String post = text.substring(removeIndex + removeLength);
		return pre + post;
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
	 * @param string
	 * 		Text with range to replace.
	 * @param start
	 * 		Range start.
	 * @param end
	 * 		Range end.
	 * @param replacement
	 * 		Replacement text to replace the contents of the given range.
	 *
	 * @return Modified string.
	 */
	public static String replaceRange(String string, int start, int end, String replacement) {
		String temp = string.substring(0, start);
		temp += replacement;
		temp += string.substring(end);
		return temp;
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
	 * @param pattern
	 * 		Pattern to look for.
	 * @param text
	 * 		Text to check.
	 *
	 * @return Number of times the given pattern appears in the text.
	 */
	public static int count(char pattern, String text) {
		int count = 0;
		int length = text.length();
		for (int i = 0; i < length; i++) {
			if (text.charAt(i) == pattern) count++;
		}
		return count;
	}

	/**
	 * @param path
	 * 		Some path.
	 *
	 * @return String of path.
	 */
	public static String pathToString(Path path) {
		return path.toString();
	}

	/**
	 * @param path
	 * 		Some path.
	 *
	 * @return String of file name at path.
	 */
	public static String pathToNameString(Path path) {
		return path.getFileName().toString();
	}

	/**
	 * @param path
	 * 		Some path.
	 *
	 * @return String of absolute path.
	 */
	public static String pathToAbsoluteString(Path path) {
		String absolutePath = path.toAbsolutePath().toString();
		if (absolutePath.indexOf('\\') > 0) // Translate windows-specific path to platform independent path name
			absolutePath = absolutePath.replace('\\', '/');
		return absolutePath;
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
	 * @param a
	 * 		Some string.
	 * @param b
	 * 		Another.
	 *
	 * @return The common prefix between the two strings.
	 */
	public static String getCommonPrefix(String a, String b) {
		int len = Math.min(a.length(), b.length());
		for (int i = 0; i < len; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, len);
	}

	/**
	 * @param left
	 * 		Some string with the ending to focus on.
	 * @param right
	 * 		Some string with the beginning to focus on.
	 *
	 * @return The common text between the two.
	 */
	public static String getIntersection(String left, String right) {
		int aLength = left.length();
		int len = Math.min(aLength, right.length());
		String match = "";
		StringBuilder sba = new StringBuilder();
		StringBuilder sbb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			String aa = sba.insert(0, left.charAt(aLength - i - 1)).toString();
			String bb = sbb.append(right.charAt(i)).toString();
			if (aa.equals(bb)) {
				match = bb;
			}
		}
		return match;
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
		sb.append(text.repeat(Math.max(0, times)));
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
		Char2IntMap charCountMap = new Char2IntArrayMap(64);
		for (char c : text.toCharArray())
			charCountMap.mergeInt(c, 1, Integer::sum);
		int length = text.length();
		final double[] entropy = {0.0};
		charCountMap.values().forEach(value -> {
			double freq = ((double) value) / length;
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
	public static String generateIncrementingName(String alphabet, int index) {
		char[] charz = alphabet.toCharArray();
		int alphabetLength = charz.length;
		int length = 8;
		final char[] array = new char[length];
		int offset = length - 1;
		while (index > charz.length - 1) {
			int k = Math.abs(-(index % alphabetLength));
			array[offset--] = charz[k];
			index /= alphabetLength;
			index -= 1;
		}
		array[offset] = charz[index];
		return new String(array, offset, length - offset);
	}

	/**
	 * Creates pseudo-random names of the desired length.
	 * Repeated calls with the same input yield the same value.
	 *
	 * @param alphabet
	 * 		The alphabet to pull from.
	 * @param length
	 * 		Desired length of generated string.
	 * @param seed
	 * 		Input seed.
	 *
	 * @return Generated name.
	 */
	public static String generateName(String alphabet, int length, int seed) {
		Random r = new Random(seed);
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length)
			sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
		return sb.toString();
	}

	/**
	 * @param data
	 * 		Some data to check.
	 *
	 * @return {@code true} when it contains only text.
	 */
	public static boolean isText(@Nonnull byte[] data) {
		if (data.length == 0)
			return false;
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int length = data.length;
		int entropy = 0;
		int bufferSize = Math.min(length, 4096);
		char[] charArray = new char[bufferSize];
		CharBuffer charBuf = CharBuffer.wrap(charArray);
		while (true) {
			try {
				if (!buffer.hasRemaining())
					return true;
				CoderResult result = decoder.decode(buffer, charBuf, true);
				if (result.isMalformed() || result.isError() || result.isUnmappable())
					return false;
				if (result.isUnderflow())
					decoder.flush(charBuf);
				entropy = calculateNonText(charArray, entropy, length, charBuf.position());
				if (entropy == -1)
					return false;
				buffer.position(Math.min(length, buffer.position() + bufferSize));
			} catch (Exception ex) {
				return false;
			}
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