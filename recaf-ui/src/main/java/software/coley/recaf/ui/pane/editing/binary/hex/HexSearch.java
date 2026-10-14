package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.collect.primitive.IntList;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Utility for searching bytes with nibble-level masking.
 *
 * @author Matt Coley
 */
public final class HexSearch {
	private HexSearch() {}

	/**
	 * Parse pairs of hexadecimal nibbles.
	 * <ul>
	 *     <li>A {@code ?} matches any nibble</li>
	 *     <li>Two {@code ??} matches any byte</li>
	 *     <li>Whitespace and commas are ignored</li>
	 * </ul>
	 *
	 * @param text
	 * 		Query such as {@code "AE0A00AF"}, {@code "AE 0A 00 AF"}, or {@code "A? ??"}.
	 *
	 * @return Parsed masked pattern. Empty text creates an empty pattern.
	 */
	@Nonnull
	public static Pattern parseHexPattern(@Nonnull String text) {
		IntList values = new IntList();
		IntList masks = new IntList();
		boolean hasToken = false;
		boolean commaPending = false;
		int index = 0;
		while (index < text.length()) {
			// Skip whitespace and commas.
			char current = text.charAt(index);
			if (Character.isWhitespace(current)) {
				index++;
				continue;
			}
			if (current == ',') {
				if (!hasToken || commaPending)
					throw invalidPattern(text);
				commaPending = true;
				index++;
				continue;
			}

			// Read token.
			int tokenStart = index;
			while (index < text.length() && !Character.isWhitespace(text.charAt(index)) && text.charAt(index) != ',')
				index++;

			// Check token length and parse nibbles.
			int tokenLength = index - tokenStart;
			if (tokenLength == 0 || (tokenLength & 1) != 0)
				throw invalidPattern(text);
			for (int i = tokenStart; i < index; i += 2) {
				char highChar = text.charAt(i);
				char lowChar = text.charAt(i + 1);
				int high = highChar == '?' ? 0 : HexUtil.hexDigit(highChar);
				int low = lowChar == '?' ? 0 : HexUtil.hexDigit(lowChar);
				if ((highChar != '?' && high < 0) || (lowChar != '?' && low < 0))
					throw invalidPattern(text);
				values.add((high << 4) | low);
				masks.add((highChar == '?' ? 0 : 0xf0) | (lowChar == '?' ? 0 : 0x0f));
			}
			hasToken = true;
			commaPending = false;
		}
		if (commaPending)
			throw invalidPattern(text);

		// Convert to arrays and return pattern.
		byte[] valueBytes = new byte[values.size()];
		byte[] maskBytes = new byte[masks.size()];
		for (int i = 0; i < valueBytes.length; i++) {
			valueBytes[i] = (byte) values.get(i);
			maskBytes[i] = (byte) masks.get(i);
		}
		return new Pattern(valueBytes, maskBytes);
	}

	/**
	 * Encode a literal 7-bit ASCII query.
	 *
	 * @param text
	 * 		Query text.
	 *
	 * @return Exact byte pattern.
	 *
	 * @throws IllegalArgumentException
	 * 		When the query contains non-ASCII characters.
	 */
	@Nonnull
	public static Pattern asciiPattern(@Nonnull String text) {
		byte[] values = new byte[text.length()];
		byte[] masks = new byte[text.length()];
		Arrays.fill(masks, (byte) 0xff);
		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if (character > 0x7f)
				throw new IllegalArgumentException("ASCII search text must contain only 7-bit characters");
			values[i] = (byte) character;
		}
		return new Pattern(values, masks);
	}

	/**
	 * Find all overlapping occurrences of a pattern.
	 *
	 * @param data
	 * 		Source bytes.
	 * @param pattern
	 * 		Search pattern.
	 *
	 * @return Ascending byte offsets.
	 */
	@Nonnull
	public static int[] findAll(@Nonnull byte[] data, @Nonnull Pattern pattern) {
		int length = pattern.values.length;
		if (length == 0 || length > data.length)
			return new int[0];

		IntList matches = new IntList();
		if (pattern.isExact())
			findExact(data, pattern.values, matches);
		else
			findMasked(data, pattern, matches);
		return matches.toArray();
	}

	/**
	 * Knuth-Morris-Pratt search for exact byte matches.
	 *
	 * @param data
	 * 		Source bytes.
	 * @param values
	 * 		Exact search pattern.
	 * @param matches
	 * 		List to populate with matching offsets.
	 */
	private static void findExact(@Nonnull byte[] data, @Nonnull byte[] values, @Nonnull IntList matches) {
		int[] failure = new int[values.length];
		for (int i = 1, matched = 0; i < values.length; i++) {
			checkCancelled(i);
			while (matched > 0 && values[i] != values[matched])
				matched = failure[matched - 1];
			if (values[i] == values[matched])
				matched++;
			failure[i] = matched;
		}

		int matched = 0;
		for (int i = 0; i < data.length; i++) {
			checkCancelled(i);
			byte value = data[i];
			while (matched > 0 && value != values[matched])
				matched = failure[matched - 1];
			if (value == values[matched])
				matched++;
			if (matched == values.length) {
				matches.add(i - values.length + 1);
				matched = failure[matched - 1];
			}
		}
	}

	/**
	 * Brute-force search for masked byte matches.
	 *
	 * @param data
	 * 		Source bytes.
	 * @param pattern
	 * 		Masked search pattern.
	 * @param matches
	 * 		List to populate with matching offsets.
	 */
	private static void findMasked(@Nonnull byte[] data, @Nonnull Pattern pattern, @Nonnull IntList matches) {
		int lastStart = data.length - pattern.values.length;
		for (int candidate = 0; candidate <= lastStart; candidate++) {
			checkCancelled(candidate);
			boolean match = true;
			for (int i = 0; i < pattern.values.length; i++) {
				checkCancelled(candidate + i);
				int mask = Byte.toUnsignedInt(pattern.masks[i]);
				if ((data[candidate + i] & mask) != (pattern.values[i] & mask)) {
					match = false;
					break;
				}
			}
			if (match)
				matches.add(candidate);
		}
	}

	/**
	 * Check if the current thread has been interrupted, and if so, throw a cancellation exception.
	 *
	 * @param scanned
	 * 		Number of bytes scanned so far. Used to throttle the check frequency.
	 */
	private static void checkCancelled(int scanned) {
		if ((scanned & 0x3ff) == 0 && Thread.currentThread().isInterrupted())
			throw new CancellationException("Hex search cancelled");
	}

	/**
	 * @param text
	 * 		Text that failed to parse.
	 *
	 * @return Exception for the invalid pattern.
	 */
	@Nonnull
	private static IllegalArgumentException invalidPattern(@Nonnull String text) {
		return new IllegalArgumentException("Invalid hexadecimal search pattern: " + text);
	}

	/**
	 * Pattern of bytes to search for, with nibble-level masking.
	 *
	 * @param values
	 * 		Byte values to match.
	 * @param masks
	 * 		Byte masks to apply to the values. A mask of {@code 0xff} means the byte must match exactly, while a mask of
	 *        {@code 0x00} means the byte is ignored. Masks must select complete nibbles, so valid masks are {@code 0x00},
	 *        {@code 0xf0}, {@code 0x0f}, and {@code 0xff}.
	 */
	public record Pattern(@Nonnull byte[] values, @Nonnull byte[] masks) {
		public Pattern {
			Objects.requireNonNull(values, "values");
			Objects.requireNonNull(masks, "masks");
			if (values.length != masks.length)
				throw new IllegalArgumentException("Pattern values and masks must have the same length");
			values = values.clone();
			masks = masks.clone();
			for (int i = 0; i < masks.length; i++) {
				int mask = Byte.toUnsignedInt(masks[i]);
				int highMask = mask & 0xf0;
				int lowMask = mask & 0x0f;
				if ((highMask != 0 && highMask != 0xf0) || (lowMask != 0 && lowMask != 0x0f))
					throw new IllegalArgumentException("Pattern masks must select complete nibbles");
				values[i] &= masks[i];
			}
		}

		@Override
		@Nonnull
		public byte[] values() {
			return values.clone();
		}

		@Override
		@Nonnull
		public byte[] masks() {
			return masks.clone();
		}

		public int length() {
			return values.length;
		}

		private boolean isExact() {
			for (byte mask : masks)
				if (mask != (byte) 0xff)
					return false;
			return true;
		}
	}
}
