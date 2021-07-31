package me.coley.recaf.ui.util;

import java.util.Arrays;

/**
 * Hex utilities.
 *
 * @author Matt Coley
 */
public class HexUtils {
    private static final int INVALID = Byte.MAX_VALUE + 1;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final int COLS_PER_LINE = 16;

    public static boolean IsHex(String text) {
        return text.length() == 2 &&
                Arrays.binarySearch(HEX_ARRAY, text.charAt(0)) >= 0 &&
                Arrays.binarySearch(HEX_ARRAY, text.charAt(1)) >= 0;
    }

    public static String ToHex(int value) {
        return new String(new char[]{
                HEX_ARRAY[(value & 0xFF) >>> 12],
                HEX_ARRAY[(value & 0xFF) >>> 8],
                HEX_ARRAY[(value & 0xFF) >>> 4],
                HEX_ARRAY[(value & 0xFF) & 0x0F]
        });
    }

    public static byte UnHex(String value) {
        return (byte) Integer.parseInt(value, 16);
    }
}
