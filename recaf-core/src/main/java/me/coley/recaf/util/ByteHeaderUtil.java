package me.coley.recaf.util;

/**
 * Simple utility with a bunch of pre-defined patterns for file headers.
 *
 * @author Matt Coley
 */
public class ByteHeaderUtil {
	private static final int WILD = Integer.MIN_VALUE;
	// Archives
	public static final int[] TAR_LZW = {0x1F, 0x9D};
	public static final int[] TAR_LZH = {0x1F, 0xA0};
	public static final int[] BZ2 = {0x42, 0x5A, 0x68};
	public static final int[] LZ = {0x4C, 0x5A, 0x49, 0x50};
	public static final int[] TAR = {0x75, 0x73, 0x74, 0x61, 0x72};
	public static final int[] GZIP = {0x1F, 0x8B};
	public static final int[] ZIP = {0x50, 0x4B};
	public static final int[] RAR = {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07};
	public static final int[] SEVEN_Z = {0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C};
	// Programs
	public static final int[] CLASS = {0xCA, 0xFE, 0xBA, 0xBE};
	public static final int[] DEX = {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00};
	public static final int[] EXE_DLL = {0x4D, 0x5A};
	public static final int[] ELF = {0x7F, 0x45, 0x4C, 0x46};
	public static final int[] DYLIB_32 = {0xCE, 0xFA, 0xED, 0xFE};
	public static final int[] DYLIB_64 = {0xCF, 0xFA, 0xED, 0xFE};
	// Images
	public static final int[] PNG = {0x89, 0x50, 0x4E, 0x47};
	public static final int[] JPG = {0xFF, 0xD8, 0xFF};
	public static final int[] GIF = {0x47, 0x49, 0x46, 0x38};
	public static final int[] BMP = {0x42, 0x4D};
	// Audio
	public static final int[] OGG = {0x4F, 0x67, 0x67, 0x53};
	public static final int[] WAV = {0x52, 0x49, 0x46, 0x46, WILD, WILD, WILD, WILD, 0x57, 0x41, 0x56, 0x45};
	public static final int[] MP3_ID3 = {0x49, 0x44, 0x33};
	public static final int[] MP3_NO_ID1 = {0xFF, 0xFB};
	public static final int[] MP3_NO_ID2 = {0xFF, 0xF2};
	public static final int[] MP3_NO_ID3 = {0xFF, 0xF3};
	// Misc
	public static final int[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};

	/**
	 * @param array
	 * 		File data to compare against.
	 * @param pattern
	 * 		The header pattern.
	 *
	 * @return {@code true} when array prefix matches pattern.
	 */
	public static boolean match(byte[] array, int... pattern) {
		int patternLen;
		if (array == null || array.length < (patternLen = pattern.length))
			return false;
		for (int i = 0; i < patternLen; i++) {
			int patternVal = pattern[i];
			if (patternVal == WILD)
				continue;
			if (array[i] != (byte) patternVal)
				return false;
		}
		return true;
	}
}
