package me.coley.recaf.assemble.ast.insn;

/**
 * Jump instruction.
 *
 * @author Matt Coley
 */
public class JumpInstruction extends AbstractInstruction {
	private final String label;

	/**
	 * @param opcode
	 * 		Jump instruction opcode.
	 * @param label
	 * 		Jump target label name.
	 */
	public JumpInstruction(String opcode, String label) {
		super(opcode);
		this.label = label;
	}

	/**
	 * @return Jump target label name.
	 */
	public String getLabel() {
		return label;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcode(), getLabel());
	}
}
