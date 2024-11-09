package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A wrapper around {@link org.objectweb.asm.Type}.
 *
 * @author Matt Coley
 */
public class Types {
	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
	public static final Type CLASS_TYPE = Type.getObjectType("java/lang/Class");
	public static final Type STRING_TYPE = Type.getObjectType("java/lang/String");
	public static final Type ARRAY_1D_BOOLEAN = Type.getObjectType("[Z");
	public static final Type ARRAY_1D_CHAR = Type.getObjectType("[C");
	public static final Type ARRAY_1D_BYTE = Type.getObjectType("[B");
	public static final Type ARRAY_1D_SHORT = Type.getObjectType("[S");
	public static final Type ARRAY_1D_INT = Type.getObjectType("[I");
	public static final Type ARRAY_1D_FLOAT = Type.getObjectType("[F");
	public static final Type ARRAY_1D_DOUBLE = Type.getObjectType("[D");
	public static final Type ARRAY_1D_LONG = Type.getObjectType("[J");
	public static final Type[] PRIMITIVES = new Type[]{
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
	public static final Collection<String> PRIMITIVE_BOXES = Arrays.asList(
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
	public static boolean isPrimitive(@Nullable Type type) {
		return type != null && type.getSort() <= Type.DOUBLE;
	}

	/**
	 * @param desc
	 * 		Some internal type descriptor.
	 *
	 * @return {@code true} if it matches a reserved primitive type.
	 */
	public static boolean isPrimitive(@Nullable String desc) {
		if (desc == null || desc.length() != 1)
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
	 * @param name
	 * 		Must be a primitive class name. See {@link #isPrimitiveClassName(String)}.
	 *
	 * @return Internal name of primitive.
	 *
	 * @throws IllegalArgumentException
	 * 		When the descriptor was not a primitive.
	 */
	@Nonnull
	public static String classToPrimitive(@Nonnull String name) {
		for (Type prim : PRIMITIVES) {
			String className = prim.getClassName();
			if (className.equals(name))
				return prim.getInternalName();
		}
		throw new IllegalArgumentException("Descriptor was not a primitive class name!");
	}

	/**
	 * @param name
	 * 		Some class name.
	 *
	 * @return {@code true} if it matches the class name of a primitive type.
	 */
	public static boolean isPrimitiveClassName(@Nullable String name) {
		if (name == null)
			return false;
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(name))
				return true;
		return false;
	}

	/**
	 * @param desc
	 * 		Class descriptor.
	 *
	 * @return {@code true} if it is one of the children of {@link Number}.
	 */
	public static boolean isBoxedPrimitive(@Nullable String desc) {
		return PRIMITIVE_BOXES.contains(desc);
	}

	/**
	 * @param type
	 * 		Some type to check.
	 *
	 * @return {@code true} if type is a void type.
	 */
	public static boolean isVoid(@Nullable Type type) {
		return type != null && type.getSort() == Type.VOID;
	}

	/**
	 * @param type
	 * 		Base type.
	 * @param dimensions
	 * 		Array dimensions.
	 *
	 * @return Array type of dimension size.
	 */
	@Nonnull
	public static Type array(@Nonnull Type type, int dimensions) {
		return Type.getType("[".repeat(dimensions) + type.getDescriptor());
	}

	/**
	 * @param methodType
	 * 		Parsed method descriptor type.
	 *
	 * @return Number of variable slots occupied by the parameters.
	 */
	public static int countParameterSlots(@Nonnull Type methodType) {
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
	public static boolean isValidDesc(@Nullable String desc) {
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
		} else if (first == 'L' || first == '[') {
			try {
				Type type = Type.getType(desc);
				if (type.getSort() == Type.OBJECT && !desc.endsWith(";"))
					return false;
				else if (type.getSort() == Type.ARRAY && type.getElementType() == null)
					return false;
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
		return false;
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if it is a wide type.
	 */
	public static boolean isWide(@Nullable Type type) {
		if (type == null) return false;
		return Type.DOUBLE_TYPE.equals(type) || Type.LONG_TYPE.equals(type);
	}

	/**
	 * @param opcode
	 * 		Some instruction opcode.
	 *
	 * @return The implied variable type, or {@code null} if the passed opcode does not imply a type.
	 */
	@Nullable
	public static Type fromVarOpcode(int opcode) {
		return switch (opcode) {
			case Opcodes.IINC, Opcodes.ILOAD, Opcodes.ISTORE -> Type.INT_TYPE;
			case Opcodes.ALOAD, Opcodes.ASTORE -> Types.OBJECT_TYPE;
			case Opcodes.FLOAD, Opcodes.FSTORE -> Type.FLOAT_TYPE;
			case Opcodes.DLOAD, Opcodes.DSTORE -> Type.DOUBLE_TYPE;
			case Opcodes.LLOAD, Opcodes.LSTORE -> Type.LONG_TYPE;
			default -> null;
		};
	}

	/**
	 * @param opcode
	 * 		Some array opcode.
	 *
	 * @return The implied variable type, or {@code null} if the passed opcode does not imply a type.
	 */
	@Nullable
	public static Type fromArrayOpcode(int opcode) {
		return switch (opcode) {
			case Opcodes.ARRAYLENGTH, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.IALOAD,
					Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.IASTORE -> Type.INT_TYPE;
			case Opcodes.AALOAD, Opcodes.AASTORE -> Types.OBJECT_TYPE;
			case Opcodes.FALOAD, Opcodes.FASTORE -> Type.FLOAT_TYPE;
			case Opcodes.DALOAD, Opcodes.DASTORE -> Type.DOUBLE_TYPE;
			case Opcodes.LALOAD, Opcodes.LASTORE -> Type.LONG_TYPE;
			default -> null;
		};
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
		else if (sort > 0 && sort < Type.INT)
			sort = Type.INT;
		return sort;
	}

	/**
	 * @param sort
	 *        {@link Type#getSort()}.
	 *
	 * @return Name of sort.
	 */
	@Nonnull
	public static String getSortName(int sort) {
		return switch (sort) {
			case Type.VOID -> "void";
			case Type.BOOLEAN -> "boolean";
			case Type.CHAR -> "char";
			case Type.BYTE -> "byte";
			case Type.SHORT -> "short";
			case Type.INT -> "int";
			case Type.FLOAT -> "float";
			case Type.LONG -> "long";
			case Type.DOUBLE -> "double";
			case Type.ARRAY -> "array";
			case Type.OBJECT -> "object";
			case Type.METHOD -> "method";
			case -1 -> "<undefined>";
			default -> "<unknown>";
		};
	}

	/**
	 * @param type
	 * 		Input type.
	 *
	 * @return Pretty-printed type.
	 */
	@Nonnull
	public static String pretty(@Nonnull Type type) {
		int sort = type.getSort();
		String suffix = null;
		String name;
		if (sort == Type.ARRAY) {
			suffix = "[]".repeat(type.getDimensions());
			type = type.getElementType();
			sort = type.getSort();
		} else if (sort == Type.METHOD) {
			List<String> args = Arrays.stream(type.getArgumentTypes())
					.map(Types::pretty)
					.toList();
			return "(" + String.join(", ", args) + ") " + pretty(type.getReturnType());
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
	 * @param signature
	 * 		Signature text.
	 * @param isTypeSignature
	 * 		See {@link org.objectweb.asm.signature.SignatureReader#accept(SignatureVisitor)} ({@code false})
	 * 		and {@link org.objectweb.asm.signature.SignatureReader#acceptType(SignatureVisitor)} ({@code true}) for usage.
	 *
	 * @return {@code true} for a valid signature. Will be {@code false} otherwise, or for {@code null} values.
	 */
	public static boolean isValidSignature(@Nullable String signature, boolean isTypeSignature) {
		if (signature == null || signature.isEmpty())
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
