package me.coley.recaf.assemble.ast.insn;

/**
 * Integer increment instruction.
 *
 * @author Matt Coley
 */
public class IincInstruction extends AbstractInstruction {
	private final String identifier;
	private final int increment;

	/**
	 * @param opcode
	 * 		Increment instruction opcode.
	 * @param identifier
	 * 		Variable identifier.
	 * @param increment
	 * 		Increment value.
	 */
	public IincInstruction(String opcode, String identifier, int increment) {
		super(opcode);
		this.identifier = identifier;
		this.increment = increment;
	}

	/**
	 * @return Variable identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return Increment value.
	 */
	public int getIncrement() {
		return increment;
	}

	@Override
	public String print() {
		return String.format("%s %s %d", getOpcode(), getIdentifier(), getIncrement());
	}
}
