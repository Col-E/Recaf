package me.coley.recaf.assemble.ast.insn;

import java.util.List;

/**
 * Table switch instruction.
 *
 * @author Matt Coley
 */
public class TableSwitchInstruction extends AbstractInstruction {
	private final int min;
	private final int max;
	private final List<String> labels;
	private final String defaultIdentifier;

	/**
	 * @param opcode
	 * 		Table switch opcode.
	 * @param min
	 * 		Range minimum.
	 * @param max
	 * 		Range maximum.
	 * @param labels
	 * 		Labels associated with range values.
	 * @param defaultIdentifier
	 * 		Default label identifier.
	 */
	public TableSwitchInstruction(String opcode, int min, int max, List<String> labels, String defaultIdentifier) {
		super(opcode);
		this.min = min;
		this.max = max;
		this.labels = labels;
		this.defaultIdentifier = defaultIdentifier;
	}

	/**
	 * @return Range minimum.
	 */
	public int getMin() {
		return min;
	}

	/**
	 * @return Range maximum.
	 */
	public int getMax() {
		return max;
	}

	/**
	 * @return Labels associated with range values.
	 */
	public List<String> getLabels() {
		return labels;
	}

	/**
	 * @return Default label identifier.
	 */
	public String getDefaultIdentifier() {
		return defaultIdentifier;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.TABLE;
	}

	@Override
	public String print() {
		String offsets = String.join(", ", labels);
		return String.format("%s range(%d:%d) offsets(%s) default(%s)",
				getOpcode(), getMin(), getMax(), offsets, getDefaultIdentifier());
	}
}
