package me.coley.recaf.bytecode;

import java.lang.reflect.Field;

import org.objectweb.asm.Type;

import me.coley.recaf.config.impl.ConfDisplay;

/**
 * Utilities for handling ASM's Type.
 * 
 * @author Matt
 */
public class TypeUtil {
	/**
	 * private sort denoting an object type, such as "com/Example" versus the
	 * standard "Lcom/Example;".
	 */
	private static final int INTERNAL = 12;
	/**
	 * Field access to Type's sort.
	 */
	private static Field sortF;

	/**
	 * @param text
	 *            Text to parse as Type.
	 * @return Type from text.
	 */
	public static Type parse(String text) {
		Type t = null;
		if (text.isEmpty()) {
			return t;
		}
		if (isPrimitive(text)) {
			t = Type.getType(text);
		} else if (isMethod(text)) {
			t = Type.getMethodType(text);
		} else if (isObject(text)) {
			t = Type.getObjectType(text);
		} else {
			t = Type.getType(text);
		}
		return t;
	}

	/**
	 * @param t
	 *            Type to convert.
	 * @return Formatted Type as string.
	 */
	public static String toString(Type t) {
		if (isInternal(t)) {
			return t.getInternalName();
		} else {
			return t.toString();
		}
	}

	/**
	 * @param s
	 *            Type to check.
	 * @return Type denotes a primitive type.
	 */
	public static boolean isPrimitive(String s) {
		if (s.length() != 1) {
			return false;
		}
		switch (s.charAt(0)) {
		case 'Z':
		case 'C':
		case 'B':
		case 'S':
		case 'I':
		case 'F':
		case 'J':
		case 'D':
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param s
	 *            Text to check.
	 * @return Type is method format, "(Ltype/args;)Lreturn;"
	 */
	public static boolean isMethod(String s) {
		// This assumes a lot, but hey, it serves our purposes.
		return s.startsWith("(");
	}

	/**
	 * @param s
	 *            Text to check.
	 * @return Type is standard format of "Lcom/Example;".
	 */
	public static boolean isStandard(String s) {
		return s.length() > 2 && s.charAt(0) == 'L' && s.charAt(s.length() - 1) == ';';
	}

	/**
	 * 
	 * @param s
	 *            Text to check.
	 * @return Type is object format of "com/Example".
	 */
	public static boolean isObject(String s) {
		return !isMethod(s) && !isStandard(s);
	}

	/**
	 * @param t
	 *            Type to check.
	 * @return Type is an object type, format of "com/Example"
	 */
	public static boolean isInternal(Type t) {
		try {
			return (int) sortF.get(t) == INTERNAL;
		} catch (Exception e) {
			return false;
		}
	}

	static {
		// get raw access to sort so we can check for INTERNAL sort.
		try {
			sortF = Type.class.getDeclaredField("sort");
			sortF.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

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
		return toString(type);
	}
}
