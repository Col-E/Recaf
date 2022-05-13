package me.coley.recaf.assemble.ast.insn;

/**
 * Integer instruction.
 *
 * @author Matt Coley
 */
public class IntInstruction extends AbstractInstruction {
	private final int value;

	/**
	 * @param opcode
	 * 		Integer instruction opcode.
	 * @param value
	 * 		Integer instruction parameter.
	 */
	public IntInstruction(int opcode, int value) {
		super(opcode);
		this.value = value;
	}

	/**
	 * @return Integer instruction parameter.
	 */
	public int getValue() {
		return value;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.INT;
	}

	@Override
	public String print() {
		return String.format("%s %d", getOpcode(), getValue());
	}
}
