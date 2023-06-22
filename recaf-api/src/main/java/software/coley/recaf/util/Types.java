package software.coley.recaf.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.Collection;

/**
 * A wrapper around {@link org.objectweb.asm.Type}.
 *
 * @author Matt Coley
 */
public class Types {
	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
	public static final Type STRING_TYPE = Type.getObjectType("java/lang/String");
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
	private static final Collection<String> PRIMITIVE_BOXES = Arrays.asList(
			"Ljava/lang/Boolean;",
			"Ljava/lang/Byte;",
			"Ljava/lang/Character;",
			"Ljava/lang/Short;",
			"Ljava/lang/Integer;",
			"Ljava/lang/Float;",
			"Ljava/lang/Double;",
			"Ljava/lang/Long;"
	);

	/**
	 * @param type
	 * 		Some type to check.
	 *
	 * @return {@code true} if it matches a primitive type.
	 */
	public static boolean isPrimitive(Type type) {
		return type != null && type.getSort() <= Type.DOUBLE;
	}

	/**
	 * @param type
	 * 		Some type to check.
	 *
	 * @return {@code true} if type is a void type.
	 */
	public static boolean isVoid(Type type) {
		return type != null && type.getSort() == Type.VOID;
	}

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
	 * @param type
	 * 		Base type.
	 * @param dimensions
	 * 		Array dimensions.
	 *
	 * @return Array type of dimension size.
	 */
	public static Type array(Type type, int dimensions) {
		return Type.getType(StringUtil.repeat("[", dimensions) + type.getDescriptor());
	}

	/**
	 * @param methodType
	 * 		Parsed method descriptor type.
	 *
	 * @return Number of variable slots occupied by the parameters.
	 */
	public static int countParameterSlots(Type methodType) {
		int size = 0;
		Type[] methodArgs = methodType.getArgumentTypes();
		for (Type arg : methodArgs)
			size += arg.getSize();
		return size;
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
				Type methodType = Type.getMethodType(desc);
				methodType.getArgumentTypes();
				methodType.getReturnType();
				return true;
			} catch (Throwable t) {
				return false;
			}
		} else {
			try {
				Type type = Type.getType(desc);
				if (type.getSort() == Type.OBJECT && !desc.endsWith(";"))
					return false;
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if it is a wide type.
	 */
	public static boolean isWide(Type type) {
		return Type.DOUBLE_TYPE.equals(type) || Type.LONG_TYPE.equals(type);
	}

	/**
	 * @param opcode
	 * 		Some instruction opcode.
	 *
	 * @return The implied variable type, or {@code null} if the passed opcode does not imply a type.
	 */
	public static Type fromVarOpcode(int opcode) {
		switch (opcode) {
			case Opcodes.IINC:
			case Opcodes.ILOAD:
			case Opcodes.ISTORE:
				return Type.INT_TYPE;
			case Opcodes.ALOAD:
			case Opcodes.ASTORE:
				return Types.OBJECT_TYPE;
			case Opcodes.FLOAD:
			case Opcodes.FSTORE:
				return Type.FLOAT_TYPE;
			case Opcodes.DLOAD:
			case Opcodes.DSTORE:
				return Type.DOUBLE_TYPE;
			case Opcodes.LLOAD:
			case Opcodes.LSTORE:
				return Type.LONG_TYPE;
			default:
				return null;
		}
	}

	/**
	 * @param opcode
	 * 		Some array opcode.
	 *
	 * @return The implied variable type, or {@code null} if the passed opcode does not imply a type.
	 */
	public static Type fromArrayOpcode(int opcode) {
		switch (opcode) {
			case Opcodes.ARRAYLENGTH:
			case Opcodes.BALOAD:
			case Opcodes.CALOAD:
			case Opcodes.SALOAD:
			case Opcodes.IALOAD:
			case Opcodes.BASTORE:
			case Opcodes.CASTORE:
			case Opcodes.SASTORE:
			case Opcodes.IASTORE:
				return Type.INT_TYPE;
			case Opcodes.AALOAD:
			case Opcodes.AASTORE:
				return Types.OBJECT_TYPE;
			case Opcodes.FALOAD:
			case Opcodes.FASTORE:
				return Type.FLOAT_TYPE;
			case Opcodes.DALOAD:
			case Opcodes.DASTORE:
				return Type.DOUBLE_TYPE;
			case Opcodes.LALOAD:
			case Opcodes.LASTORE:
				return Type.LONG_TYPE;
			default:
				return null;
		}
	}

	/**
	 * @param sort
	 * 		Some type sort.
	 *
	 * @return Normalized sort. This is in the context of runtime expectations.
	 * Any type smaller than {@code int} is treated as an {@code int}.
	 * Array types are essentially drop-in replaceable with object types in most cases.
	 */
	public static int getNormalizedSort(int sort) {
		if (sort == Type.ARRAY)
			sort = Type.OBJECT;
		else if (sort < Type.INT)
			sort = Type.INT;
		return sort;
	}

	/**
	 * @param sort
	 *        {@link Type#getSort()}.
	 *
	 * @return Name of sort.
	 */
	public static String getSortName(int sort) {
		switch (sort) {
			case Type.VOID:
				return "void";
			case Type.BOOLEAN:
				return "boolean";
			case Type.CHAR:
				return "char";
			case Type.BYTE:
				return "byte";
			case Type.SHORT:
				return "short";
			case Type.INT:
				return "int";
			case Type.FLOAT:
				return "float";
			case Type.LONG:
				return "long";
			case Type.DOUBLE:
				return "double";
			case Type.ARRAY:
				return "array";
			case Type.OBJECT:
				return "object";
			case Type.METHOD:
				return "method";
			case -1:
				return "<undefined>";
			default:
				return "<UNKNOWN>";
		}
	}

	/**
	 * @param type
	 * 		Input type.
	 *
	 * @return Pretty-printed type.
	 */
	public static String pretty(Type type) {
		int sort = type.getSort();
		String suffix = null;
		String name;
		if (sort == Type.ARRAY) {
			suffix = StringUtil.repeat("[]", type.getDimensions());
			type = type.getElementType();
			sort = type.getSort();
		}
		if (sort <= Type.DOUBLE) {
			name = getSortName(sort);
		} else {
			name = type.getInternalName();
		}
		String pretty = StringUtil.shortenPath(name);
		if (suffix != null) {
			pretty += suffix;
		}
		return pretty;
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
	 * @return Internal name of primitive.
	 */
	public static String classToPrimitive(String desc) {
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(desc))
				return prim.getInternalName();
		throw new IllegalArgumentException("Descriptor was not a primitive class name!");
	}

	/**
	 * @param desc
	 * 		Class descriptor.
	 *
	 * @return {@code true} if it is one of the children of {@link Number}.
	 */
	public static boolean isBoxedPrimitive(String desc) {
		return PRIMITIVE_BOXES.contains(desc);
	}

	/**
	 * @param signature
	 * 		Signature text.
	 * @param isTypeSignature
	 * 		See {@code org.objectweb.asm.commons.ClassRemapper} for usage.
	 *
	 * @return {@code true} for signature being parsing.
	 */
	public static boolean isValidSignature(String signature, boolean isTypeSignature) {
		if (signature == null)
			return false;
		try {
			SignatureReader signatureReader = new SignatureReader(signature);
			SignatureWriter signatureWriter = new SignatureWriter();
			if (isTypeSignature) {
				signatureReader.acceptType(signatureWriter);
			} else {
				signatureReader.accept(signatureWriter);
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}
