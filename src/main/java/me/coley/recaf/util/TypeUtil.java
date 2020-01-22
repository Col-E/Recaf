package me.coley.recaf.util;

import org.objectweb.asm.Type;

/**
 * Utilities for ASM's {@link Type} class <i>(And some additional descriptor cases)</i>
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
	 * @param desc
	 *            Type to check.
	 * @return Type denotes a primitive type.
	 */
	public static boolean isPrimitiveDesc(String desc) {
		if(desc.length() != 1) {
			return false;
		}
		switch(desc.charAt(0)) {
			case 'Z':
			case 'C':
			case 'B':
			case 'S':
			case 'I':
			case 'F':
			case 'J':
			case 'D':
				return true;
			default:
				return false;
		}
	}

	/**
	 *
	 * @param desc
	 *            Text to check.
	 * @return {@code true} when the descriptor is in method format, "(Ltype/args;)Lreturn;"
	 */
	public static boolean isMethodDesc(String desc) {
		// This assumes a lot, but hey, it serves our purposes.
		return desc.charAt(0) == '(';
	}

	/**
	 * @param desc
	 *            Text to check.
	 * @return {@code true} when the descriptor is in standard format, "Lcom/Example;".
	 */
	public static boolean isFieldDesc(String desc) {
		return desc.length() > 2 && desc.charAt(0) == 'L' && desc.charAt(desc.length() - 1) == ';';
	}

	/**
	 *
	 * @param desc
	 *            Text to check.
	 * @return Type is object/internal format of "com/Example".
	 */
	public static boolean isInternal(String desc) {
		return !isMethodDesc(desc) && !isFieldDesc(desc);
	}

	/**
	 * Convert a Type sort to a string representation.
	 *
	 * @param sort
	 * 		Type sort value.
	 *
	 * @return Sort string value.
	 */
	public static String sortToString(int sort) {
		switch(sort) {
			case Type.VOID:
				return "VOID";
			case Type.BOOLEAN:
				return "BOOLEAN";
			case Type.CHAR:
				return "CHAR";
			case Type.BYTE:
				return "BYTE";
			case Type.SHORT:
				return "SHORT";
			case Type.INT:
				return "INT";
			case Type.FLOAT:
				return "FLOAT";
			case Type.LONG:
				return "LONG";
			case Type.DOUBLE:
				return "DOUBLE";
			case Type.ARRAY:
				return "ARRAY";
			case Type.OBJECT:
				return "OBJECT";
			case Type.METHOD:
				return "METHOD";
			case INTERNAL:
				return "INTERNAL";
			default:
				return "UNKNOWN";
		}
	}

	/**
	 * @param sort
	 * 		Type sort<i>(kind)</i>
	 *
	 * @return Size of type.
	 */
	public static int sortToSize(int sort) {
		switch(sort) {
			case Type.LONG:
			case Type.DOUBLE:
				return 2;
			default:
				return 1;
		}
	}

	/**
	 * @param type
	 * 		Some array type.
	 *
	 * @return Array depth.
	 */
	public static int getArrayDepth(Type type) {
		int i = 0;
		while(type.getSort() == Type.ARRAY) {
			++i;
			type = type.getElementType();
		}
		return i;
	}

	/**
	 * @param type
	 * 		Some array type.
	 *
	 * @return Array element type.
	 */
	public static Type getElementType(Type type) {
		while(type.getSort() == Type.ARRAY)
			type = type.getElementType();
		return type;
	}
}
