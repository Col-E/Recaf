package me.coley.recaf.util;

/**
 * Parsing utilities.
 * 
 * @author Matt
 */
public class Parse {
	/**
	 * @param s
	 *            Text to check.
	 * @return Does text represent an integer.
	 */
	public static boolean isInt(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @param s
	 *            Text to check.
	 * @return Does text represent a boolean.
	 */
	public static boolean isBoolean(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Boolean.parseBoolean(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}