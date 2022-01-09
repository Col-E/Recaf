package me.coley.recaf.assemble.ast.insn;

/**
 * Line number instruction.
 *
 * @author Matt Coley
 */
public class LineInstruction extends AbstractInstruction {
	private final String label;
	private final int lineNo;

	/**
	 * @param opcode
	 * 		Increment instruction opcode.
	 * @param label
	 * 		Label name.
	 * @param lineNo
	 * 		Line number value.
	 */
	public LineInstruction(String opcode, String label, int lineNo) {
		super(opcode, -1);
		this.label = label;
		this.lineNo = lineNo;
	}

	/**
	 * @return Label name.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return Line number value.
	 */
	public int getLineNo() {
		return lineNo;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.LINE;
	}

	@Override
	public String print() {
		return String.format("%s %s %d", getOpcode(), getLabel(), getLineNo());
	}
}
