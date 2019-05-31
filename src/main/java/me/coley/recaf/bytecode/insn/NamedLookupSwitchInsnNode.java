package me.coley.recaf.bytecode.insn;

import me.coley.recaf.parse.assembly.exception.LabelLinkageException;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Extension of LookupSwitchInsnNode that allows for labels to be linked to
 * identifier strings. This allows serialized labeled offsets in bytecode to be
 * deserialized into valid offsets to satisfy the super-type's label array.
 * 
 * @author Matt
 */
public class NamedLookupSwitchInsnNode extends LookupSwitchInsnNode implements NamedLabelRefInsn {
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

	public NamedLookupSwitchInsnNode(String dfltLabelId, String[] labelIds, int[] keys) {
		this(dfltLabelId, null, labelIds, keys, null);
	}

	public NamedLookupSwitchInsnNode(String dfltLabelId, LabelNode dflt, String[] labelIds, int[] keys,
									 LabelNode[] labels) {
		super(dflt, keys, labels);
		this.dfltLabelId = dfltLabelId;
		this.labelsIdentifiers = labelIds;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labelMapping) {
		return new NamedLookupSwitchInsnNode(dfltLabelId, labelMapping.get(dflt), labelsIdentifiers,
				keys.stream().mapToInt(i -> i).toArray(), getUpdatedLabels(labelMapping));
	}

	@Override
	public AbstractInsnNode cleanClone(final Map<LabelNode, LabelNode> labelMapping) {
		return new LookupSwitchInsnNode(labelMapping.get(dflt), keys.stream().mapToInt(i -> i).toArray(), getUpdatedLabels(labelMapping));
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

	@Override
	public void setupLabels(Map<String, LabelNode> labels) {
		dflt = labels.get(dfltLabelId);
		if(dflt == null)
			throw new LabelLinkageException(this, "Label identifier has no mapped value: " +
					dfltLabelId);
		this.labels.clear();
		for(String id : labelsIdentifiers) {
			LabelNode lbl = labels.get(id);
			if(lbl == null)
				throw new LabelLinkageException(this, "Label identifier has no mapped value: " + id);
			this.labels.add(lbl);
		}
	}
}