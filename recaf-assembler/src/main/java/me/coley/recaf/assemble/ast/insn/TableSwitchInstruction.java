package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.FlowControl;
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
	public String print() {
		StringBuilder builder = new StringBuilder();
		builder.append(getOpcode()).append(" range(");
		builder.append(getMin()).append(':').append(getMax()).append(") ");
		builder.append("offsets(");
		List<String> labels = this.labels;
		if (!labels.isEmpty()) {
			int i = 0;
			for (int j = labels.size() - 1; i < j; i++) {
				builder.append(labels.get(i)).append(", ");
			}
			builder.append(labels.get(i));
		}
		builder.append(") default(");
		builder.append(getDefaultIdentifier()).append(')');
		return builder.toString();
	}

	@Override
	public boolean isForced() {
		// A switch must go to one of the flow targets
		return true;
	}
}
