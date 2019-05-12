package me.coley.recaf.bytecode.insn;

import me.coley.recaf.parse.assembly.exception.LabelLinkageException;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Extension of TableSwitchInsnNode that allows for labels to be linked to
 * identifier strings. This allows serialized labeled offsets in bytecode to be
 * deserialized into valid offsets to satisfy the super-type's label array.
 * 
 * @author Matt
 */
public class NamedTableSwitchInsnNode extends TableSwitchInsnNode implements NamedLabelRefInsn {
	/**
	 * Placeholder identifier for default label. The label is typically known
	 * after instantiation, thus making it impossible to provide in the
	 * constructor.
	 */
	private final String dfltLabelId;
	/**
	 * Same as {@link #dfltLabelId} but for the destination labels.
	 */
	private final String[] labelIds;

	public NamedTableSwitchInsnNode(int min, int max, String dfltLabelId, String[] labelIds) {
		this(min, max, dfltLabelId, null, labelIds, null);
	}

	public NamedTableSwitchInsnNode(int min, int max, String dfltLabel, LabelNode dflt, String[] labelIds,
									LabelNode[] labels) {
		super(min, max, dflt, labels);
		this.dfltLabelId = dfltLabel;
		this.labelIds = labelIds;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labelMapping) {
		return new NamedTableSwitchInsnNode(min, max, dfltLabelId, labelMapping.get(dflt), labelIds, getUpdatedLabels(labelMapping));
	}

	@Override
	public AbstractInsnNode cleanClone(final Map<LabelNode, LabelNode> labelMapping) {
		return new TableSwitchInsnNode(min, max, labelMapping.get(dflt), getUpdatedLabels(labelMapping));
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
			throw new LabelLinkageException(this, "Label identifier has no mapped value: " + dfltLabelId);
		this.labels.clear();
		for(String id : labelIds) {
			LabelNode lbl = labels.get(id);
			if (lbl == null)
				throw new LabelLinkageException(this, "Label identifier has no mapped value: " + id);
			this.labels.add(lbl);
		}
	}
}