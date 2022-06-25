package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;

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
	public String print(PrintContext context) {
		return getOpcode() + " " + getValue();
	}
}
