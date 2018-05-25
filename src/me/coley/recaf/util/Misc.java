package me.coley.recaf.util;

import org.objectweb.asm.Type;

import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.config.impl.ConfDisplay;

/**
 * Temporary location for {@link #filter(Type)}.
 * 
 * @author Matt
 *
 */
public class Misc {

	/**
	 * Filters a non-method descriptor based on display configuration
	 * <i>(simplification)</i>
	 * 
	 * @param type
	 *            Input type.
	 * @return Filtered version of given type.
	 */
	public static String filter(Type type) {
		if (type.getSort() == Type.METHOD) {
			Thread.dumpStack();
		}
		if (ConfDisplay.instance().simplify) {
			if (type.getSort() == Type.ARRAY) {
				String base = filter(type.getElementType());
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < type.getDimensions(); i++) {
					sb.append("[]");
				}
				return base + sb.toString();
			}
			String name = type.getClassName();
			if (name.contains(".")) {
				// substring package name away
				name = name.substring(name.lastIndexOf(".") + 1);
			}
			return name;
		}
		// No simplification
		return TypeUtil.toString(type);
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
	public static Class<?> getType(String desc) {
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
