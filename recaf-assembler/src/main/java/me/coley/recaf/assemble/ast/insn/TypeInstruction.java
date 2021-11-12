package me.coley.recaf.assemble.ast.insn;

/**
 * Type instruction.
 *
 * @author Matt Coley
 */
public class TypeInstruction extends AbstractInstruction {
	private final String type;

	/**
	 * @param opcode
	 * 		Type instruction opcode.
	 * @param type
	 * 		Type descriptor.
	 */
	public TypeInstruction(String opcode, String type) {
		super(opcode);
		this.type = type;
	}

	/**
	 * @return Type descriptor.
	 */
	public String getType() {
		return type;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.TYPE;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcode(), getType());
	}
}
