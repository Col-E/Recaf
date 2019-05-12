package me.coley.recaf.bytecode.insn;

import me.coley.recaf.parse.assembly.exception.LabelLinkageException;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Extension of JumpInsnNode where the label can be set after instantiation. In
 * cases where the label is not yet known an identifier is used as a
 * placeholder. Later, when the block of opcodes is read a label should exist
 * that pertains to the placeholder identifier.
 * 
 * @author Matt
 */
public class NamedJumpInsnNode extends JumpInsnNode implements NamedLabelRefInsn {
	/**
	 * Placeholder identifier for a label. The label is typically known after
	 * instantiation, thus making it impossible to provide in the constructor.
	 */
	private final String labelId;

	public NamedJumpInsnNode(int opcode, String labelIdentifier) {
		this(opcode, null, labelIdentifier);
	}

	public NamedJumpInsnNode(int opcode, LabelNode label, String labelId) {
		super(opcode, label);
		this.labelId = labelId;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new NamedJumpInsnNode(opcode, labels.get(label), labelId);
	}

	@Override
	public AbstractInsnNode cleanClone(final Map<LabelNode, LabelNode> labels) {
		return new JumpInsnNode(opcode, labels.get(label));
	}

	@Override
	public void setupLabels(Map<String, LabelNode> labels) {
		LabelNode lbl = labels.get(labelId);
		if (lbl == null)
			throw new LabelLinkageException(this, "Label identifier has no mapped value: " + labelId);
		label = lbl;
	}
}