package me.coley.recaf.util;

import org.objectweb.asm.Type;

/**
 * A wrapper around {@link org.objectweb.asm.Type}.
 *
 * @author Matt Coley
 */
public class Types {
	/**
	 * @param desc
	 * 		Some internal type descriptor.
	 *
	 * @return {@code true} if it matches a reserved primitive type.
	 */
	public static boolean isPrimitive(String desc) {
		if (desc.length() != 1)
			return false;
		char c = desc.charAt(0);
		switch (c) {
			case 'V':
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
	 * ASM likes to throw {@link IllegalArgumentException} in cases where it can't parse type descriptors.
	 * This lets us check beforehand if its valid.
	 *
	 * @param desc
	 * 		Descriptor to check.
	 *
	 * @return {@code true} when its parsable.
	 */
	@SuppressWarnings("all")
	public static boolean isValidDesc(String desc) {
		if (desc == null)
			return false;
		if (desc.length() == 0)
			return false;
		char first = desc.charAt(0);
		if (first == '(') {
			try {
				Type.getMethodType(desc);
				return true;
			} catch (Throwable t) {
				return false;
			}
		} else {
			try {
				Type.getType(desc);
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}
}
