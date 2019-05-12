package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Extended instructions with names to indicate placeholder labels.
 *
 * @author Matt
 */
public interface NamedLabelRefInsn {
	/**
	 * Setup the labels based off of the given mapping of placeholder names to actual label
	 * instances.
	 *
	 * @param labels
	 * 		&lt;Identifier : Instance&gt;
	 */
	void setupLabels(final Map<String, LabelNode> labels);

	/**
	 * @return Cloned instruction without serialization information. Uses the standard instruction
	 * isntance.
	 */
	AbstractInsnNode cleanClone(final Map<LabelNode, LabelNode> labels);

	// ========================= UTILITY METHODS ========================================= //

	/**
	 * Populate the instructions labels, assuming they are {@link NamedLabelRefInsn} instances.
	 *
	 * @param labels
	 * 		Map of label names to instances.
	 * @param insns
	 * 		Array of instructions to setup named labels with the given label map.
	 */
	static void setupLabels(Map<String, LabelNode> labels, AbstractInsnNode[] insns) {
		for(AbstractInsnNode ain : insns) {
			if(ain instanceof NamedLabelRefInsn) {
				NamedLabelRefInsn named = (NamedLabelRefInsn) ain;
				named.setupLabels(labels);
			}
		}
	}

	/**
	 * @param labels
	 * 		Map of label names to instances.
	 * @param insns
	 * 		Collection of instructions to setup named labels with the given label map.
	 */
	static void setupLabels(Map<String, LabelNode> labels, Collection<AbstractInsnNode> insns) {
		setupLabels(labels, insns.toArray(new AbstractInsnNode[0]));
	}

	/**
	 * Replace the named instructions with the standard implementations.
	 *
	 * @param labels
	 * 		Map of labels, keys are {@link NamedLabelNode} which are
	 * 		to be replaced by standard LabelNodes.
	 * @param insns
	 * 		Instruction list containing the labels and references to them.
	 *
	 * @return Updated instruction list with replaced label references.
	 */
	static InsnList clean(Map<LabelNode, LabelNode> labels, InsnList insns) {
		InsnList copy = new InsnList();
		for(AbstractInsnNode ain : insns.toArray()) {
			if(ain instanceof NamedLabelRefInsn) {
				copy.add(((NamedLabelRefInsn) ain).cleanClone(labels));
			} else if(ain instanceof LabelNode) {
				copy.add(labels.get(ain));
			} else {
				copy.add(ain);
			}
		}
		return copy;
	}

	/**
	 * @param insns
	 * 		Array of instructions.
	 *
	 * @return Map of label names to the label instances.
	 */
	static Map<String, LabelNode> getLabels(AbstractInsnNode[] insns) {
		Map<String, LabelNode> map = new HashMap<>();
		for(AbstractInsnNode ain : insns)
			if(ain.getOpcode() == NamedLabelNode.NAMED_LABEL) {
				NamedLabelNode label = (NamedLabelNode) ain;
				map.put(label.name, label);
			}
		return map;
	}

	/**
	 * @param insns
	 * 		Collection of instructions.
	 *
	 * @return Map of label names to the label instances.
	 */
	static Map<String, LabelNode> getLabels(Collection<AbstractInsnNode> insns) {
		return getLabels(insns.toArray(new AbstractInsnNode[0]));
	}
}
