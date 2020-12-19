package me.coley.recaf.util;

import org.objectweb.asm.Type;

/**
 * Utilities for ASM's {@link Type} class <i>(And some additional descriptor cases)</i>
 *
 * @author Matt
 */
public class TypeUtil {
	private static final Type[] PRIMITIVES = new Type[]{
		Type.VOID_TYPE,
		Type.BOOLEAN_TYPE,
		Type.BYTE_TYPE,
		Type.CHAR_TYPE,
		Type.SHORT_TYPE,
		Type.INT_TYPE,
		Type.FLOAT_TYPE,
		Type.DOUBLE_TYPE,
		Type.LONG_TYPE
	};

	/**
	 * Cosntant for object type.
	 */
	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

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
	 * @param arg
	 * 		Operand value of a NEWARRAY instruction.
	 *
	 * @return Array element type.
	 */
	public static Type newArrayArgToType(int arg) {
		switch(arg) {
			case 4: return Type.BOOLEAN_TYPE;
			case 5: return Type.CHAR_TYPE;
			case 6: return Type.FLOAT_TYPE;
			case 7: return Type.DOUBLE_TYPE;
			case 8: return Type.BYTE_TYPE;
			case 9: return Type.SHORT_TYPE;
			case 10: return Type.INT_TYPE;
			case 11: return Type.LONG_TYPE;
			default: break;
		}
		throw new IllegalArgumentException("Unexpected NEWARRAY arg: " + arg);
	}

	/**
	 * @param type
	 * 		Array element type.
	 *
	 * @return Operand value for a NEWARRAY instruction.
	 */
	public static int typeToNewArrayArg(Type type) {
		switch(type.getDescriptor().charAt(0)) {
			case 'Z': return 4;
			case 'C': return 5;
			case 'F': return 6;
			case 'D': return 7;
			case 'B': return 8;
			case 'S': return 9;
			case 'I': return 10;
			case 'J': return 11;
			default: break;
		}
		throw new IllegalArgumentException("Unexpected NEWARRAY type: " + type.getDescriptor());
	}

	/**
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
		if (type.getSort() == Type.ARRAY)
			return type.getDimensions();
		return 0;
	}

	/**
	 * @param desc
	 * 		Some class name.
	 *
	 * @return {@code true} if it matches the class name of a primitive type.
	 */
	public static boolean isPrimitiveClassName(String desc) {
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(desc))
				return true;
		return false;
	}

	/**
	 * @param desc
	 * 		Must be a primitive class name. See {@link #isPrimitiveClassName(String)}.
	 *
	 * @return Internal name.
	 */
	public static String classToPrimitive(String desc) {
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(desc))
				return prim.getInternalName();
		throw new IllegalArgumentException("Descriptor was not a primitive class name!");
	}
}
