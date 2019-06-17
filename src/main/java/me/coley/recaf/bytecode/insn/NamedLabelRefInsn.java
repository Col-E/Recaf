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
	 * @param method
	 * 		Method to setup named labels with the given label map.
	 */
	static void setupLabels(Map<String, LabelNode> labels, MethodNode method) {
		for(AbstractInsnNode ain : method.instructions.toArray()) {
			if(ain instanceof NamedLabelRefInsn) {
				NamedLabelRefInsn named = (NamedLabelRefInsn) ain;
				named.setupLabels(labels);
			}
		}
	}

	/**
	 * Populate the instructions labels, assuming they are {@link NamedLabelRefInsn} instances.
	 *
	 * @param labels
	 * 		Map of label names to instances.
	 * @param block
	 * 		Block of instructions to setup named labels with the given label map.
	 */
	static void setupLabels(Map<String, LabelNode> labels, List<AbstractInsnNode> block) {
		for(AbstractInsnNode ain : block) {
			if(ain instanceof NamedLabelRefInsn) {
				NamedLabelRefInsn named = (NamedLabelRefInsn) ain;
				named.setupLabels(labels);
			}
		}
	}

	/**
	 * Replace the named instructions with the standard implementations.
	 *
	 * @param labels
	 * 		Map of labels, keys are {@link NamedLabelNode} which are
	 * 		to be replaced by standard LabelNodes.
	 * @param method
	 * 		Method containing instructions that need to be purged of debug instructions
	 *
	 * @return Map of replaced instructions.
	 */
	static Map<AbstractInsnNode, AbstractInsnNode> clean(Map<LabelNode, LabelNode> labels,
														 MethodNode method) {
		Map<AbstractInsnNode, AbstractInsnNode> replacements = new HashMap<>();
		InsnList copy = new InsnList();
		for(AbstractInsnNode ain : method.instructions.toArray()) {
			AbstractInsnNode replace;
			if(ain instanceof NamedLabelRefInsn) {
				NamedLabelRefInsn ref = (NamedLabelRefInsn) ain;
				copy.add(replace = ref.cleanClone(labels));
			} else if(ain instanceof LabelNode) {
				// TODO: Why does the 'labels' map not have a mapping for this instruction?
				// Looking at this method's usage, this case should NEVER be called in the
				// assembler since labels should be compiled to NamedLabelRefInsn...
				copy.add(replace = labels.getOrDefault(ain, new LabelNode()));
			} else {
				copy.add(replace = ain.clone(labels));
			}
			replacements.put(ain, replace);
		}
		method.instructions = copy;
		return replacements;
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
			if(ain instanceof NamedLabelNode) {
				NamedLabelNode label = (NamedLabelNode) ain;
				map.put(label.name, label);
			}
		return map;
	}

	/**
	 * @param method
	 * 		Method with instructions containing labels.
	 *
	 * @return Map of label names to the label instances.
	 */
	static Map<String, LabelNode> getLabels(MethodNode method) {
		return getLabels(method.instructions.toArray());
	}

	/**
	 * @param block
	 * 		Instructions containing labels.
	 *
	 * @return Map of label names to the label instances.
	 */
	static Map<String, LabelNode> getLabels(List<AbstractInsnNode> block) {
		return getLabels(block.toArray(new AbstractInsnNode[0]));
	}
}
