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

	@Override
	public String print() {
		return String.format("%s '%s'", getOpcode(), arrayType);
	}
}
