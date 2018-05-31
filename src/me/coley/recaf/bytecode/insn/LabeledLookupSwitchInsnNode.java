package me.coley.recaf.bytecode.insn;

import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;

/**
 * Extension of LookupSwitchInsnNode that allows for labels to be linked to
 * identifier strings. This allows serialized labeled offsets in bytecode to be
 * deserialized into valid offsets to satisfy the super-type's label array.
 * 
 * @author Matt
 */
public class LabeledLookupSwitchInsnNode extends LookupSwitchInsnNode {
	/**
	 * Placeholder identifier for default label. The label is typically known
	 * after instantiation, thus making it impossible to provide in the
	 * constructor.
	 */
	private final String labelIdentifier;
	/**
	 * Same as {@link #labelIdentifier} but for the destination labels.
	 */
	private final String[] labelsIdentifiers;

	public LabeledLookupSwitchInsnNode(String labelIdentifier, String[] labelsIdentifiers, int[] keys) {
		this(labelIdentifier, null, labelsIdentifiers, keys, null);
	}

	public LabeledLookupSwitchInsnNode(String labelIdentifier, LabelNode dflt, String[] labelsIdentifiers, int[] keys,
			LabelNode[] labels) {
		super(dflt, keys, labels);
		this.labelIdentifier = labelIdentifier;
		this.labelsIdentifiers = labelsIdentifiers;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new LabeledLookupSwitchInsnNode(labelIdentifier, labels.get(dflt), labelsIdentifiers, keys.stream().mapToInt(
				i -> i).toArray(), getLabels(labels));
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
		for(String id : labelsIdentifiers) {
			this.labels.add(labels.get(id));
		}
	}
}