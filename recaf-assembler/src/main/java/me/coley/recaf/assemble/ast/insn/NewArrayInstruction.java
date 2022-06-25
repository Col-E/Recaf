package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Instruction for allocating a new array of a primitive type.
 * <br>
 * Technically this could be a {@link IntInstruction} but since the assembler format
 * is supposed to make things easier it will be based off the actual characters rather
 * than the literal value <i>(which is what ASM does)</i>.
 *
 * @author Matt Coley
 */
public class NewArrayInstruction extends AbstractInstruction {
	// boolean  Z
	// char     C
	// float    F
	// double   D
	// byte     B
	// short    S
	// int      I
	// long     J
	private static final Map<String, Integer> newArrayTypes = new HashMap<>();
	private static final Map<Integer, String> newArrayNames = new HashMap<>();
	private static final Map<String, Character> newArrayChars = new HashMap<>();
	private final String arrayType;

	/**
	 * @param opcode
	 * 		Opcode name.
	 * @param arrayType
	 * 		Char representing the array type.
	 */
	public NewArrayInstruction(int opcode, String arrayType) {
		super(opcode);
		this.arrayType = arrayType;
	}

	/**
	 * @return Char representing the array type.
	 */
	public String getArrayType() {
		return arrayType;
	}

	/**
	 * The int value is defined in the table listed in
	 * <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html#jvms-6.5.newarray">
	 * 6.5. Instructions, newarray</a>.
	 *
	 * @return Int value used by the class file format representing the array type.
	 */
	public int getArrayTypeInt() {
		// From 'jvms-6.5.newarray' in the specification
		return newArrayTypes.get(getArrayType());
	}

	/**
	 * @return Char representing the primitive type of the array type.
	 */
	public char getArrayTypeChar() {
		return newArrayChars.get(getArrayType());
	}

	/**
	 * @param value
	 * 		Int value used by the class file format representing the array type.
	 *
	 * @return Character representing primitive type of array to generate.
	 */
	public static String fromInt(int value) {
		return newArrayNames.get(value);
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.NEWARRAY;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + " " + newArrayNames.get(getArrayTypeInt());
	}

	static {
		newArrayTypes.put("byte", T_BYTE);
		newArrayTypes.put("short", T_SHORT);
		newArrayTypes.put("int", T_INT);
		newArrayTypes.put("long", T_LONG);
		newArrayTypes.put("float", T_FLOAT);
		newArrayTypes.put("double", T_DOUBLE);
		newArrayTypes.put("char", T_CHAR);
		newArrayTypes.put("boolean", T_BOOLEAN);
		newArrayNames.put(T_BYTE, "byte");
		newArrayNames.put(T_SHORT, "short");
		newArrayNames.put(T_INT, "int");
		newArrayNames.put(T_LONG, "long");
		newArrayNames.put(T_FLOAT, "float");
		newArrayNames.put(T_DOUBLE, "double");
		newArrayNames.put(T_CHAR, "char");
		newArrayNames.put(T_BOOLEAN, "boolean");
		newArrayChars.put("byte", 'B');
		newArrayChars.put("short", 'S');
		newArrayChars.put("int", 'I');
		newArrayChars.put("long", 'J');
		newArrayChars.put("float", 'F');
		newArrayChars.put("double", 'D');
		newArrayChars.put("char", 'C');
		newArrayChars.put("boolean", 'Z');
	}
}
