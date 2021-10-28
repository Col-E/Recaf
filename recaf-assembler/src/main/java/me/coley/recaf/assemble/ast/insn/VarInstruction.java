package me.coley.recaf.assemble.ast.insn;

/**
 * Variable instruction.
 *
 * @author Matt Coley
 */
public class VarInstruction extends AbstractInstruction {
	private final String identifier;

	/**
	 * @param opcode
	 * 		Variable instruction opcode.
	 * @param identifier
	 * 		Variable identifier.
	 */
	public VarInstruction(String opcode, String identifier) {
		super(opcode);
		this.identifier = identifier;
	}

	/**
	 * @return Variable identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcode(), getIdentifier());
	}
}
