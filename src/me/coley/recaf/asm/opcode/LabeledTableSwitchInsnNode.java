package me.coley.recaf.asm.opcode;

import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

public class LabeledTableSwitchInsnNode extends TableSwitchInsnNode {
	/**
	 * Placeholder identifier for default label. The label is typically known
	 * after instantiation, thus making it impossible to provide in the
	 * constructor.
	 */
	private final String labelIdentifier;
	/**
	 * Same as {@link #labelIdentifier} but for the destination labels.
	 */
	private final String[] labelIdentifiers;

	public LabeledTableSwitchInsnNode(int min, int max, String labelIdentifier, String[] labelIdentifiers) {
		this(min, max, labelIdentifier, null, labelIdentifiers, null);
	}

	public LabeledTableSwitchInsnNode(int min, int max, String labelIdentifier, LabelNode dflt, String[] labelIdentifiers,
			LabelNode[] labels) {
		super(min, max, dflt, labels);
		this.labelIdentifier = labelIdentifier;
		this.labelIdentifiers = labelIdentifiers;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new LabeledTableSwitchInsnNode(min, max, labelIdentifier, labels.get(dflt), labelIdentifiers, getLabels(labels));
	}

	private LabelNode[] getLabels(Map<LabelNode, LabelNode> labels) {
		LabelNode[] l = new LabelNode[this.labels.size()];
		for (int i = 0; i < this.labels.size(); i++) {
			l[i] = labels.get(this.labels.get(i));
		}
		return l;
	}

	/**
	 * Set the default label and destination lalaeels with a map of label
	 * identifiers to their instances.
	 * 
	 * @param labels
	 *            &lt;Identifier : Instance&gt;
	 */
	public void setupLabels(Map<String, LabelNode> labels) {
		dflt = labels.get(labelIdentifier);
		this.labels.clear();
		for (int i = 0; i < labelIdentifiers.length; i++) {
			this.labels.add(labels.get(labelIdentifiers[i]));
		}
	}
}
