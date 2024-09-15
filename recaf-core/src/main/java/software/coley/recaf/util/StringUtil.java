package software.coley.recaf.util;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Various utilities for {@link String} manipulation.
 *
 * @author Matt Coley
 */
public class StringUtil {
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final int[] EMPTY_INT_ARRAY = new int[0];

	/**
	 * @param input
	 * 		Some text to search.
	 * @param c
	 * 		Char to get index of in the input string.
	 *
	 * @return Array of indices in the input string where the char exists.
	 */
	public static int[] indicesOf(@Nonnull String input, char c) {
		IntList list = new IntArrayList(5);
		int i = -1;
		do {
			i = input.indexOf(c, i);
			if (i >= 0) {
				list.add(i);
				i++;
			}
		} while (i >= 0);
		return list.toArray(EMPTY_INT_ARRAY);
	}

	/**
	 * @param input
	 * 		Some text to split.
	 * @param includeEmpty
	 *        {@code true} to include empty strings.
	 * @param split
	 * 		Split character.
	 *
	 * @return List of strings between the split characters.
	 */
	@Nonnull
	public static List<String> fastSplit(@Nonnull String input, boolean includeEmpty, char split) {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> words = new ArrayList<>();
		words.ensureCapacity(input.length() / 5);
		char[] strArray = input.toCharArray();
		for (char c : strArray) {
			if (c == split) {
				if ((includeEmpty || !sb.isEmpty())) words.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		if ((includeEmpty || !sb.isEmpty())) words.add(sb.toString());
		return words;
	}

	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 */
	@Nonnull
	public static String[] splitNewline(@Nonnull String input) {
		return input.split("\n\r|\r\n|\n");
	}

	/**
	 * @param input
	 * 		Some text containing newlines.
	 *
	 * @return Input split by newline.
	 * Empty lines <i>(Containing only the newline split)</i> are omitted.
	 */
	@Nonnull
	public static String[] splitNewlineSkipEmpty(@Nonnull String input) {
		String[] split = input.split("[\r\n]+");
		// If the first line of the file is a newline split will still have
		// one blank entry at the start.
		if (split.length > 1 && split[0].isEmpty())
			return Arrays.copyOfRange(split, 1, split.length);
		return split;
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Character to match up to.
	 *
	 * @return Input text, up until the first cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtFirst(@Nonnull String text, char cutoff) {
		int i = text.indexOf(cutoff);
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Sequence to match up to.
	 *
	 * @return Input text, up until the first cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtFirst(@Nonnull String text, @Nonnull String cutoff) {
		int i = text.indexOf(cutoff);
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Character to match up to.
	 *
	 * @return Input text, up until the last cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtLast(@Nonnull String text, char cutoff) {
		int i = text.lastIndexOf(cutoff);
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Sequence to match up to.
	 *
	 * @return Input text, up until the last cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtLast(@Nonnull String text, @Nonnull String cutoff) {
		int i = text.lastIndexOf(cutoff);
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Character to match up to.
	 * @param n
	 * 		Times to match the character.
	 *
	 * @return Input text, up until the last cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtNth(@Nonnull String text, char cutoff, int n) {
		int i = -1;
		while (n-- > 0) {
			if ((i = text.indexOf(cutoff, i + 1)) < 0)
				return text;
		}
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param cutoff
	 * 		Character to match up to.
	 * @param n
	 * 		Times to match the character.
	 *
	 * @return Input text, up until the last cutoff sequence.
	 */
	@Nonnull
	public static String cutOffAtNth(@Nonnull String text, String cutoff, int n) {
		int i = -1;
		while (n-- > 0) {
			if ((i = text.indexOf(cutoff, i + 1)) < 0)
				return text;
		}
		if (i < 0) return text;
		return text.substring(0, i);
	}

	/**
	 * @param text
	 * 		Input text.
	 * @param after
	 * 		Sequence to find and use as a cut-off point.
	 *
	 * @return Input text, after the occurrence of the given pattern.
	 */
	@Nonnull
	public static String getAfter(@Nonnull String text, @Nonnull String after) {
		int i = text.lastIndexOf(after);
		if (i < 0) return text;
		return text.substring(i + after.length());
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
	@Nonnull
	public static String insert(@Nonnull String text, int insertIndex, @Nullable String insertion) {
		if (insertion == null || insertion.isEmpty() || insertIndex < 0 || insertIndex > text.length()) return text;
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
	@Nonnull
	public static String remove(@Nonnull String text, int removeIndex, int removeLength) {
		if (removeIndex < 0 || removeIndex >= text.length()) return text;
		String pre = text.substring(0, removeIndex);
		String post = text.substring(Math.min(removeIndex + removeLength, text.length()));
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
	@Nonnull
	public static String withEmptyFallback(@Nullable String defaultText, @Nonnull String fallback) {
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
	public static boolean isDecimal(@Nullable String text) {
		if (text == null || text.isEmpty())
			return false;
		try {
			Double.parseDouble(text);
			return true;
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
	@Nonnull
	public static String replaceLast(@Nonnull String string, @Nonnull String toReplace, @Nonnull String replacement) {
		int i = string.lastIndexOf(toReplace);
		if (i > -1)
			return string.substring(0, i) + replacement + string.substring(i + toReplace.length());
		return string;
	}

	/**
	 * @param string
	 * 		Text containing prefix to replace.
	 * @param oldPrefix
	 * 		Old prefix.
	 * @param newPrefix
	 * 		New prefix to replace with.
	 *
	 * @return Modified string.
	 */
	@Nonnull
	public static String replacePrefix(@Nonnull String string, @Nullable String oldPrefix, @Nonnull String newPrefix) {
		if (isNullOrEmpty(oldPrefix))
			return newPrefix + string;
		if (string.startsWith(oldPrefix))
			return newPrefix + string.substring(oldPrefix.length());
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
	@Nonnull
	public static String replaceRange(@Nonnull String string, int start, int end, String replacement) {
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
	public static int count(@Nonnull String pattern, @Nullable String text) {
		if (text == null || text.isEmpty())
			return 0;
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
	public static int count(char pattern, @Nullable String text) {
		if (text == null || text.isEmpty())
			return 0;
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
	@Nonnull
	public static String pathToString(@Nonnull Path path) {
		return path.toString();
	}

	/**
	 * @param path
	 * 		Some path.
	 *
	 * @return String of file name at path.
	 */
	@Nonnull
	public static String pathToNameString(@Nonnull Path path) {
		return path.getFileName().toString();
	}

	/**
	 * @param path
	 * 		Some path.
	 *
	 * @return String of absolute path.
	 */
	@Nonnull
	public static String pathToAbsoluteString(@Nonnull Path path) {
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
	@Nonnull
	public static String shortenPath(@Nonnull String name) {
		int separatorIndex = name.lastIndexOf('/');
		if (separatorIndex > 0)
			name = name.substring(separatorIndex + 1);
		return name;
	}

	/**
	 * @param name
	 * 		Path name.
	 *
	 * @return Path name without file extension.
	 */
	@Nonnull
	public static String removeExtension(@Nonnull String name) {
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex >= 0)
			return name.substring(0, dotIndex);
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
	@Nonnull
	public static String limit(@Nonnull String text, int max) {
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
	@Nonnull
	public static String limit(@Nonnull String text, @Nullable String cutoffPattern, int max) {
		if (max <= 0) return "";
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
	@Nonnull
	public static String lowercaseFirstChar(@Nonnull String name) {
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
	@Nonnull
	public static String uppercaseFirstChar(@Nonnull String name) {
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
	@Nonnull
	public static String getCommonPrefix(@Nonnull String a, @Nonnull String b) {
		int len = Math.min(a.length(), b.length());
		for (int i = 0; i < len; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, len);
	}

	/**
	 * @param a
	 * 		Some string.
	 * @param b
	 * 		Another.
	 *
	 * @return The common suffix between the two strings.
	 */
	@Nonnull
	public static String getCommonSuffix(@Nonnull String a, @Nonnull String b) {
		int alen = a.length();
		int blen = b.length();
		int commonLength = Math.min(alen, blen);

		// Base case where one string is empty.
		if (commonLength == 0)
			return "";

		// Find fist non-equal char and get the suffix string.
		for (int i = 1; i < commonLength + 1; i++) {
			if (a.charAt(alen - i) != b.charAt(blen - i)) {
				return a.substring(alen - i + 1);
			}
		}

		// For the shared common length both strings are equal.
		// Yield the shorter string.
		return alen < blen ? a : b;
	}

	/**
	 * @param text
	 * 		Text to compute length of.
	 *
	 * @return Length of text, considering tabs.
	 */
	public static int getTabAdjustedLength(@Nonnull String text) {
		return getTabAdjustedLength(text, 4);
	}

	/**
	 * @param text
	 * 		Text to compute length of.
	 * @param tabWidth
	 * 		Tab width.
	 *
	 * @return Length of text, considering tabs.
	 */
	public static int getTabAdjustedLength(@Nonnull String text, int tabWidth) {
		int tabIndex = text.indexOf('\t');
		while (tabIndex >= 0) {
			if (tabIndex == 0) {
				text = " ".repeat(tabWidth) + text.substring(1);
			} else {
				// Assuming tab width is four: Having two spaces then a tab still yields a length of 4 visually
				// Thus, we must consider such alignment in our length adjustments.
				String pre = text.substring(0, tabIndex);
				String post = text.substring(tabIndex + 1);
				int alignedLengthExtra = tabWidth - (tabIndex % tabWidth);
				text = pre + " ".repeat(alignedLengthExtra) + post;
			}
			tabIndex = text.indexOf('\t');
		}
		return text.length();
	}

	/**
	 * @param text
	 * 		Text to scan prefix of.
	 *
	 * @return Number of blank spaces until some non-whitespace char is found.
	 */
	public static int getWhitespacePrefixLength(@Nonnull String text) {
		return getWhitespacePrefixLength(text, 4);
	}

	/**
	 * @param text
	 * 		Text to scan prefix of.
	 * @param tabWidth
	 * 		Width of spaces to translate tab characters to.
	 *
	 * @return Number of blank spaces until some non-whitespace char is found.
	 */
	public static int getWhitespacePrefixLength(@Nonnull String text, int tabWidth) {
		char[] chars = text.toCharArray();
		int offset = 0;
		for (char c : chars) {
			if (c == ' ' || c == '\t') offset++;
			else break;
		}
		return getTabAdjustedLength(text.substring(0, offset), tabWidth);
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
	@Nonnull
	public static String fillLeft(int len, @Nonnull String pattern, @Nullable String string) {
		StringBuilder sb = new StringBuilder(string == null ? "" : string);
		while (sb.length() < len)
			sb.insert(0, pattern);
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
	@Nonnull
	public static String fillRight(int len, @Nonnull String pattern, @Nullable String string) {
		StringBuilder sb = new StringBuilder(string == null ? "" : string);
		while (sb.length() < len)
			sb.append(pattern);
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
	public static double getEntropy(@Nullable String text) {
		if (text == null || text.isEmpty())
			return 0;
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
	@Nonnull
	public static String traceToString(@Nonnull Throwable t) {
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
	@Nonnull
	public static String toHexString(int value) {
		return toHexString(value, -1, 1);
	}

	/**
	 * Wrapper for {@link Integer#toHexString(int)} that pads zeros when needed.
	 *
	 * @param value
	 * 		Value to print.
	 * @param mask
	 * 		Mask to apply to the value.
	 *
	 * @return Hex printed value.
	 */
	@Nonnull
	public static String toHexString(int value, int mask) {
		return toHexString(value, mask, 1);
	}

	/**
	 * Wrapper for {@link Integer#toHexString(int)} that pads zeros when needed.
	 *
	 * @param value
	 * 		Value to print.
	 * @param mask
	 * 		Mask to apply to the value.
	 * @param minLength
	 * 		Minimum length of hex output. Left-padded with zeros to match.
	 *
	 * @return Hex printed value.
	 */
	@Nonnull
	public static String toHexString(int value, int mask, int minLength) {
		return StringUtil.fillLeft(minLength, "0", Integer.toHexString(value & mask));
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
	@Nonnull
	public static String generateIncrementingName(@Nonnull String alphabet, int index) {
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
	@Nonnull
	public static String generateName(@Nonnull String alphabet, int length, int seed) {
		Random r = new Random(seed);
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length)
			sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
		return sb.toString();
	}

	/**
	 * @param data
	 * 		Some data to decode.
	 *
	 * @return String decoding result. Check {@link StringDecodingResult#couldDecode()} to determine if successful.
	 */
	@Nonnull
	public static StringDecodingResult decodeString(@Nonnull byte[] data) {
		// It would be wrong to say we know this data is supposed to be text if its empty.
		// It's up to the caller to use context clues like file extensions to figure this case out.
		if (data.length == 0)
			return failedDecoding(data);

		// Check for byte-order-mark
		StringDecodingResult result = null;
		if (data.length >= 4) {
			if (ByteHeaderUtil.match(data, ByteHeaderUtil.TEXT_BOM_UTF_32BE)) {
				result = decodeString(data, StandardCharsets.UTF_32BE);
			} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.TEXT_BOM_UTF_32LE)) {
				result = decodeString(data, StandardCharsets.UTF_32LE);
			} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.TEXT_BOM_UTF_16BE)) {
				result = decodeString(data, StandardCharsets.UTF_16BE);
			} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.TEXT_BOM_UTF_16LE)) {
				result = decodeString(data, StandardCharsets.UTF_16BE);
			} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.TEXT_BOM_UTF_8)) {
				result = decodeString(data, StandardCharsets.UTF_8);
			}

			// If that BOM specifies a charset that works then we're good to go.
			if (result != null && result.couldDecode())
				return result;
		}

		// From 'https://stackoverflow.com/questions/3584069/is-it-possible-to-detect-text-file-encoding-of-two-possible'
		//
		// It's not possible with 100% accuracy because, for example, the bytes C3 B1 are an equally valid
		// representation of "Ăą" in ISO-8859-2 as they are of "ñ" in UTF-8. In fact, because ISO-8859-2 assigns
		// a character to all 256 possible bytes, every UTF-8 string is also a valid ISO-8859-2 string
		// (representing different characters if non-ASCII).
		//
		// However, the converse is not true. UTF-8 has strict rules about what sequences are valid.
		// More than 99% of possible 8-octet sequences are not valid UTF-8. And your CSV files are probably much
		// longer than that. Because of this, you can get good accuracy if you:
		//
		//  1. Perform a UTF-8 validity check. If it passes, assume the data is UTF-8.
		//  2. Otherwise, assume it's ISO-8859 (Latin).
		result = decodeUtf8(data);
		if (result.couldDecode())
			return result;
		return decodeLatin(data);
	}

	/**
	 * @param data
	 * 		Some data to decode.
	 *
	 * @return String decoding result. Check {@link StringDecodingResult#couldDecode()} to determine if successful.
	 */
	@Nonnull
	public static StringDecodingResult decodeUtf8(@Nonnull byte[] data) {
		return decodeString(data, StandardCharsets.UTF_8);
	}

	/**
	 * @param data
	 * 		Some data to decode.
	 *
	 * @return String decoding result. Check {@link StringDecodingResult#couldDecode()} to determine if successful.
	 */
	@Nonnull
	@SuppressWarnings("DataFlowIssue")
	public static StringDecodingResult decodeLatin(@Nonnull byte[] data) {
		StringDecodingResult result = decodeString(data, StandardCharsets.ISO_8859_1);

		// ISO_8859_1 should be a mapping of one byte to one char, and if it is not we throw it out as being invalid.
		//  - Chars < 32 will be discarded since they are special control chars and not indented for display, triggering this.
		String text = result.text();
		if (result.couldDecode() && data.length != text.length())
			return failedDecoding(data);

		return result;
	}

	/**
	 * @param data
	 * 		Some data to decode.
	 * @param encoding
	 * 		Encoding to decode with.
	 *
	 * @return String decoding result. Check {@link StringDecodingResult#couldDecode()} to determine if successful.
	 */
	@Nonnull
	public static StringDecodingResult decodeString(@Nonnull byte[] data, @Nonnull Charset encoding) {
		int bytesToChars = 1;
		if (encoding == StandardCharsets.UTF_8)
			bytesToChars = 2;
		else if (encoding == StandardCharsets.UTF_16 || encoding == StandardCharsets.UTF_16BE || encoding == StandardCharsets.UTF_16LE)
			bytesToChars = 2;
		else if (encoding == StandardCharsets.UTF_32 || encoding == StandardCharsets.UTF_32BE || encoding == StandardCharsets.UTF_32LE)
			bytesToChars = 4;
		return decodingResult(data, encoding, bytesToChars);
	}

	/**
	 * @param data
	 * 		Some data to decode.
	 * @param encoding
	 * 		Encoding to decode with.
	 * @param bytesToChars
	 * 		Expected number of bytes to make up a single {@code char} in the decoded output.
	 * 		Strictly used to reduce calls to {@link StringBuilder#ensureCapacity(int)}.
	 *
	 * @return String decoding result. Check {@link StringDecodingResult#couldDecode()} to determine if successful.
	 */
	@Nonnull
	private static StringDecodingResult decodingResult(@Nonnull byte[] data, @Nonnull Charset encoding, int bytesToChars) {
		CharsetDecoder decoder = encoding.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int totalChars = 0;
		int totalTextChars = 0;
		int length = data.length;
		int bufferSize = Math.min(length, 4096);
		char[] charArray = new char[bufferSize];
		CharBuffer charBuf = CharBuffer.wrap(charArray);
		StringBuilder output = new StringBuilder(data.length / bytesToChars);
		while (true) {
			try {
				// Exit when no remaining chars to decode
				if (!buffer.hasRemaining())
					break;

				// Decode next chunk into buffer
				CoderResult result = decoder.decode(buffer, charBuf, true);
				if (result.isMalformed() || result.isError() || result.isUnmappable())
					return failedDecoding(data);
				if (result.isUnderflow())
					decoder.flush(charBuf);

				// Check each character
				int arrayEnd = charBuf.position();
				for (int i = 0; i < arrayEnd; i++) {
					char c = charArray[i];
					int type = Character.getType(c);
					boolean isTextChar = switch (type) {
						case Character.CONTROL -> (c == '\n' || c == '\r');
						case Character.FORMAT -> EscapeUtil.isWhitespaceChar(c);
						case Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED -> false;
						default -> true;
					};
					if (isTextChar) totalTextChars++;
				}
				output.append(charArray, 0, arrayEnd);
				totalChars += arrayEnd;

				// If we overflowed in our result, we still have more to decode with this
				// current input buffer, so we should clear the output buffer and continue as-is.
				if (result.isOverflow()) {
					charBuf.flip();
					continue;
				}

				// Ensure buffer position increments to next place, but does not exceed the wrapped array's length.
				buffer.position(Math.min(length, totalChars));

				// Reset the char-buffer contents.
				charBuf.flip();

				// If we underflowed in our result we are most likely done.
				if (result.isUnderflow())
					break;
			} catch (Exception ex) {
				return failedDecoding(data);
			}
		}

		// This isn't great but works well enough for now.1
		// Basically, if most of the content is text we'll call it text even if there is some that isn't totally valid.
		if (((double) totalTextChars / totalChars) > 0.9)
			return new StringDecodingResult(data, encoding, output.toString());
		return failedDecoding(data);
	}

	@Nonnull
	private static StringDecodingResult failedDecoding(@Nonnull byte[] data) {
		return new StringDecodingResult(data, null, null);
	}
}