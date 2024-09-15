package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Simple utility with a bunch of pre-defined patterns for file headers.
 *
 * @author Matt Coley
 */
public class ByteHeaderUtil {
	private static final int WILD = Integer.MIN_VALUE;
	// JVM/Android
	public static final int[] CLASS = {0xCA, 0xFE, 0xBA, 0xBE};
	public static final int[] DEX = {0x64, 0x65, 0x78, 0x0A};
	// Archives
	public static final int[] TAR_LZW = {0x1F, 0x9D};
	public static final int[] TAR_LZH = {0x1F, 0xA0};
	public static final int[] BZ2 = {0x42, 0x5A, 0x68};
	public static final int[] LZ = {0x4C, 0x5A, 0x49, 0x50};
	public static final int[] TAR = {0x75, 0x73, 0x74, 0x61, 0x72};
	public static final int[] GZIP = {0x1F, 0x8B};
	public static final int[] ZIP = {0x50, 0x4B, 0x03, 0x04};
	public static final int[] ZIP_EMPTY = {0x50, 0x4B, 0x05, 0x06};
	public static final int[] ZIP_SPANNED = {0x50, 0x4B, 0x07, 0x08};
	public static final int[] RAR = {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07};
	public static final int[] SEVEN_Z = {0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C};
	public static final int[] JMOD = {0x4A, 0x4D};
	public static final int[] MODULES = {0xDA, 0xDA, 0xFE, 0xCA};
	public static final List<int[]> ARCHIVE_HEADERS = List.of(
			TAR_LZW,
			TAR_LZH,
			BZ2,
			LZ,
			TAR,
			GZIP,
			ZIP,
			ZIP_EMPTY,
			ZIP_SPANNED,
			RAR,
			SEVEN_Z,
			JMOD);
	// Programs
	public static final int[] PE = {0x4D, 0x5A};
	public static final int[] ELF = {0x7F, 0x45, 0x4C, 0x46};
	public static final int[] ELF_AR = {0x21, 0x3C, 0x61, 0x72, 0x63, 0x68, 0x3E, 0x0A};
	public static final int[] DYLIB_32 = {0xCE, 0xFA, 0xED, 0xFE};
	public static final int[] DYLIB_64 = {0xCF, 0xFA, 0xED, 0xFE};
	public static final List<int[]> PROGRAM_HEADERS = List.of(
			PE,
			ELF,
			ELF_AR,
			DYLIB_32,
			DYLIB_64);
	// Images
	public static final int[] PNG = {0x89, 0x50, 0x4E, 0x47};
	public static final int[] JPG = {0xFF, 0xD8, 0xFF};
	public static final int[] GIF = {0x47, 0x49, 0x46, 0x38};
	public static final int[] BMP = {0x42, 0x4D};
	public static final int[] ICO = {0x00, 0x00, 0x01, 0x00};
	public static final List<int[]> IMAGE_HEADERS = List.of(
			PNG,
			JPG,
			GIF,
			BMP,
			ICO);
	// Audio
	public static final int[] OGG = {0x4F, 0x67, 0x67, 0x53};
	public static final int[] WAV = {0x52, 0x49, 0x46, 0x46, WILD, WILD, WILD, WILD, 0x57, 0x41, 0x56, 0x45};
	public static final int[] MP3_ID3 = {0x49, 0x44, 0x33};
	public static final int[] MP3_NO_ID1 = {0xFF, 0xFB};
	public static final int[] MP3_NO_ID2 = {0xFF, 0xF2};
	public static final int[] MP3_NO_ID3 = {0xFF, 0xF3};
	public static final int[] M4A = {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x4D, 0x34, 0x41};
	public static final int[] M4ADash = {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x64, 0x61, 0x73};
	public static final List<int[]> AUDIO_HEADERS = List.of(
			OGG,
			WAV,
			MP3_ID3, MP3_NO_ID1, MP3_NO_ID2, MP3_NO_ID3,
			M4A,
			M4ADash);
	// Video
	// For MP4/QuickTime see: https://www.ftyps.com/
	public static final int[] MP4_FTYP = {WILD, WILD, WILD, WILD, 0x66, 0x74, 0x79, 0x70};
	public static final int[] MKV = {0x1A, 0x45, 0xDF, 0xA3, 0xA3, 0x42, 0x86, 0x81, 0x01, 0x42, 0xF7, 0x81};
	public static final List<int[]> VIDEO_HEADERS = List.of(
			MP4_FTYP,
			MKV
	);
	// Android files (Modular chunk system, use unsigned short LE headers)
	public static final int[] BINARY_XML = {0x03, 0x00};
	public static final int[] ARSC = {0x02, 0x00};
	// Text BOM
	public static final int[] TEXT_BOM_UTF_32BE = {0x00, 0x00, 0xFE, 0xFF};
	public static final int[] TEXT_BOM_UTF_32LE = {0xFF, 0xFE, 0x00, 0x00};
	public static final int[] TEXT_BOM_UTF_16BE = {0xFE, 0xFF, WILD, WILD};
	public static final int[] TEXT_BOM_UTF_16LE = {0xFF, 0xFE, WILD, WILD};
	public static final int[] TEXT_BOM_UTF_8 = {0xEF, 0xBB, 0xBF, WILD};
	// Misc
	public static final int[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};

	/**
	 * The reason why {@code int[]} is used is for simple unsigned byte values.
	 * This will convert the arrays into the signed {@code byte[]}.
	 *
	 * @param array
	 * 		Input array.
	 *
	 * @return Byte array.
	 */
	@Nonnull
	public static byte[] convert(int[] array) {
		byte[] copy = new byte[array.length];
		for (int i = 0; i < array.length; i++)
			copy[i] = (byte) array[i];
		return copy;
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param patterns
	 * 		The header patterns to check against.
	 *
	 * @return {@code true} when array prefix matches one of the patterns.
	 */
	public static boolean matchAny(byte[] array, List<int[]> patterns) {
		return matchAny(array, 0, patterns);
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param offset
	 * 		Offset into the data to check at.
	 * @param patterns
	 * 		The header patterns to check against.
	 *
	 * @return {@code true} when array prefix matches one of the patterns.
	 */
	public static boolean matchAny(byte[] array, int offset, List<int[]> patterns) {
		return getMatch(array, offset, patterns) != null;
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param patterns
	 * 		The header patterns to check against.
	 *
	 * @return The pattern in the list matched against the array.
	 * {@code null} if no match was found.
	 */
	@Nullable
	public static int[] getMatch(byte[] array, List<int[]> patterns) {
		return getMatch(array, 0, patterns);
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param offset
	 * 		Offset into the data to check at.
	 * @param patterns
	 * 		The header patterns to check against.
	 *
	 * @return The pattern in the list matched against the array.
	 * {@code null} if no match was found.
	 */
	@Nullable
	public static int[] getMatch(byte[] array, int offset, List<int[]> patterns) {
		for (int[] pattern : patterns) {
			if (match(array, offset, pattern)) {
				return pattern;
			}
		}
		return null;
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return {@code true} when array prefix matches pattern.
	 */
	public static boolean match(byte[] array, int... pattern) {
		return match(array, 0, pattern);
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param offset
	 * 		Offset into the data to check at.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return {@code true} when array prefix matches pattern.
	 */
	public static boolean match(byte[] array, int offset, int... pattern) {
		return matchLength(array, offset, pattern) == pattern.length;
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param offset
	 * 		Offset into the data to check at.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return Length of the pattern matched by the data in the array at the given offset.
	 * If the result is the same length as the input pattern, it is a full match.
	 */
	public static int matchLength(byte[] array, int offset, int... pattern) {
		int patternLen = pattern.length;
		if (array == null || array.length < offset + patternLen)
			return 0;
		for (int i = 0; i < patternLen; i++) {
			int patternVal = pattern[i];
			if (patternVal == WILD)
				continue;
			if (array[offset + i] != (byte) patternVal)
				return Math.max(0, i - 1);
		}
		return patternLen;
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return {@code true} when array contains the pattern.
	 */
	public static boolean matchAtAnyOffset(byte[] array, int... pattern) {
		return matchAtAnyOffset(array, array.length, pattern);
	}

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param offsetLimit
	 * 		Maximum offset to scan up to for matches.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return {@code true} when array contains the pattern.
	 */
	public static boolean matchAtAnyOffset(byte[] array, int offsetLimit, int... pattern) {
		int patternLength = pattern.length;
		int length = array.length;
		int end = Math.min(length - patternLength, offsetLimit);
		int offset = 0;
		while (offset < end) {
			int matched = matchLength(array, offset, pattern);
			if (matched == patternLength)
				return true;
			offset += Math.max(1, matched);
		}
		return false;
	}
}