package me.coley.recaf.util;

import org.objectweb.asm.Type;


import me.coley.recaf.bytecode.TypeUtil;

/**
 * Temporary location for {@link TypeUtil#filter(Type)}.
 * 
 * @author Matt
 *
 */
public class Misc {
	
	/**
	 * @return Runtime has JDK classes loaded.
	 */
	public static boolean isJDK() {
		try {
			com.sun.tools.attach.VirtualMachine.class.toString();
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	/**
	 * Lower-cases the text, keeps first letter upper-case.
	 * 
	 * @param text
	 *            Text to change case of.
	 * @return Formatted text.
	 */
	public static String fixCase(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
	}

	/**
	 * @param clazz
	 *            Class to check.
	 * @return {@code true} if class represents a number.
	 */
	public static boolean isNumeric(Class<?> clazz) {
		//@formatter:off
		return Number.class.isAssignableFrom(clazz) || 
			clazz.equals(int.class)   || 
			clazz.equals(long.class)  ||
			clazz.equals(byte.class)  ||
			clazz.equals(short.class) ||
			clazz.equals(char.class)  ||
			clazz.equals(float.class) || 
			clazz.equals(double.class);
		//@formatter:on
	}

	/**
	 * Get type from descriptor, used only for FieldNode#desc.
	 * 
	 * @param desc
	 * @return
	 */
	public static Class<?> getTypeClass(String desc) {
		switch (desc) {
		case "B":
		case "C":
		case "I":
			return int.class;
		case "J":
			return long.class;
		case "F":
			return float.class;
		case "D":
			return double.class;
		case "Ljava/lang/String;":
			return String.class;
		default:
			return null;
		}
	}

}
