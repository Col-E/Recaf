package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.meta.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Table switch instruction.
 *
 * @author Matt Coley
 */
public class TableSwitchInstruction extends AbstractInstruction implements FlowControl {
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
	public TableSwitchInstruction(int opcode, int min, int max, List<String> labels, String defaultIdentifier) {
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
	public List<Label> getTargets(Map<String, Label> labelMap) throws IllegalAstException {
		List<Label> labels = new ArrayList<>();
		for (String name : getLabels()) {
			Label label = labelMap.get(name);
			if (label == null)
				throw new IllegalAstException(this, "Could not find instance for label: " + name);
			labels.add(label);
		}
		Label label = labelMap.get(defaultIdentifier);
		if (label == null)
			throw new IllegalAstException(this, "Could not find instance for label: " + defaultIdentifier);
		labels.add(label);
		return labels;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.TABLE;
	}

	@Override
	public String print(PrintContext context) {
		String offsets = String.join("\n\t\t", labels);
		return String.format("%s %d %d\n\t\t%s\n\t\t%s %s",
				context.fmtKeyword(getOpcode()), getMin(), getMax(), offsets,
				context.fmtKeyword("default"), getDefaultIdentifier());
	}

	@Override
	public boolean isForced() {
		// A switch must go to one of the flow targets
		return true;
	}
}
