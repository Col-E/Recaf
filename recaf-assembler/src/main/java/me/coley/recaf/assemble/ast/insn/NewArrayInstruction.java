package me.coley.recaf.assemble.ast.insn;

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
	private final char arrayType;

	/**
	 * @param opcode
	 * 		Opcode name.
	 * @param arrayType
	 * 		Char representing the array type.
	 */
	public NewArrayInstruction(String opcode, char arrayType) {
		super(opcode);
		this.arrayType = arrayType;
	}

	/**
	 * @return Char representing the array type.
	 */
	public char getArrayType() {
		return arrayType;
	}

	/**
	 * @return Int value used by the class file format representing the array type.
	 */
	public int getArrayTypeInt() {
		// From 'jvms-6.5.newarray' in the specification
		switch (arrayType) {
			case 'Z':
				return 4;
			case 'C':
				return 5;
			case 'F':
				return 6;
			case 'D':
				return 7;
			case 'B':
				return 8;
			case 'S':
				return 9;
			case 'I':
				return 10;
			case 'J':
				return 11;
			default:
				throw new IllegalStateException("An invalid internal value was set: " + arrayType);
		}
	}

	/**
	 * @param value
	 * 		Int value used by the class file format representing the array type.
	 *
	 * @return Character representing primitive type of array to generate.
	 */
	public static char fromInt(int value) {
		switch (value) {
			case 4:
				return 'Z';
			case 5:
				return 'C';
			case 6:
				return 'F';
			case 7:
				return 'D';
			case 8:
				return 'B';
			case 9:
				return 'S';
			case 10:
				return 'I';
			case 11:
				return 'J';
			default:
				throw new IllegalStateException("Cannot convert to NEWARRAY type: " + value);
		}
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.NEWARRAY;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcode(), arrayType);
	}
}
