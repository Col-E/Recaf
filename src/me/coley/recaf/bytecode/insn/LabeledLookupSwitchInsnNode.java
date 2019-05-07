package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

import java.util.Map;

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
	private final String dfltLabelId;
	/**
	 * Same as {@link #dfltLabelId} but for the destination labels.
	 */
	private final String[] labelsIdentifiers;

	public LabeledLookupSwitchInsnNode(String dfltLabelId, String[] labelIds, int[] keys) {
		this(dfltLabelId, null, labelIds, keys, null);
	}

	public LabeledLookupSwitchInsnNode(String dfltLabelId, LabelNode dflt, String[] labelIds, int[] keys,
			LabelNode[] labels) {
		super(dflt, keys, labels);
		this.dfltLabelId = dfltLabelId;
		this.labelsIdentifiers = labelIds;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labelMapping) {
		return new LabeledLookupSwitchInsnNode(dfltLabelId, labelMapping.get(dflt), labelsIdentifiers,
				keys.stream().mapToInt(i -> i).toArray(), getUpdatedLabels(labelMapping));
	}

	/**
	 * Assumed that {@link #setupLabels(Map)} has been called and the {@link #labels} list has
	 * been populated.
	 *
	 * @param labelMapping
	 * 		Map of old labels to new labels.
	 *
	 * @return Updated labeles array.
	 */
	private LabelNode[] getUpdatedLabels(Map<LabelNode, LabelNode> labelMapping) {
		LabelNode[] l = new LabelNode[this.labels.size()];
		for(int i = 0; i < this.labels.size(); i++)
			l[i] = labelMapping.get(this.labels.get(i));
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
		dflt = labels.get(dfltLabelId);
		if(dflt == null)
			throw new IllegalStateException("Label identifier has no mapped value: " +
					dfltLabelId);
		this.labels.clear();
		for(String id : labelsIdentifiers) {
			LabelNode lbl = labels.get(id);
			if(lbl == null)
				throw new IllegalStateException("Label identifier has no mapped value: " + id);
			this.labels.add(lbl);
		}
	}
}