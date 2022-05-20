package me.coley.recaf.assemble.ast.insn;

/**
 * Instruction with no arguments.
 *
 * @author Matt Coley
 */
public class Instruction extends AbstractInstruction {
	/**
	 * @param opcode
	 * 		Opcode name.
	 */
	public Instruction(int opcode) {
		super(opcode);
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.INSN;
	}

	@Override
	public String print() {
		return getOpcode();
	}
}
