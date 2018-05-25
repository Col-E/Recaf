package me.coley.recaf.bytecode.insn;

import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

/**
 * Extension of JumpInsnNode where the label can be set after instantiation. In
 * cases where the label is not yet known an identifier is used as a
 * placeholder. Later, when the block of opcodes is read a label should exist
 * that pertains to the placeholder identifier.
 * 
 * @author Matt
 */
public class LabeledJumpInsnNode extends JumpInsnNode {
	/**
	 * Placeholder identifier for a label. The label is typically known after
	 * instantiation, thus making it impossible to provide in the constructor.
	 */
	private final String labelIdentifier;

	public LabeledJumpInsnNode(int opcode, String labelIdentifier) {
		this(opcode, null, labelIdentifier);
	}

	public LabeledJumpInsnNode(int opcode, LabelNode label, String labelIdentifier) {
		super(opcode, label);
		this.labelIdentifier = labelIdentifier;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new LabeledJumpInsnNode(opcode, labels.get(label), labelIdentifier);// .cloneAnnotations(this);
	}

	/**
	 * Set the label with a map of label identifiers to their instances.
	 * 
	 * @param labels
	 *            &lt;Identifier : Instance&gt;
	 */
	public void setupLabel(Map<String, LabelNode> labels) {
		label = labels.get(labelIdentifier);
	}
}