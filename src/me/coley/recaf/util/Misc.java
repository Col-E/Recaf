package me.coley.recaf.util;

import org.objectweb.asm.Type;

public class Misc {
	/**
	 * Creates a string incrementing in numerical value.
	 * 
	 * Example: a, b, c, ... z, aa, ab ...
	 * 
	 * @param index
	 *            Name index.
	 * 
	 * @return Generated String
	 */
	public static String generateName(int index) {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
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
	 * Parse type, if cannot be parsed return null.
	 * 
	 * @param typeStr
	 *            Type to parse.
	 * @return Type
	 */
	public static Type parseType(String typeStr) {
		try {
			return Type.getType(typeStr);
		} catch (Exception e) {}
		return null;
	}
}
