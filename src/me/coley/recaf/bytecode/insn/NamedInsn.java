package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

/**
 * Extended instructions with names to indicate placeholder labels.
 *
 * @author Matt
 */
public interface NamedInsn {
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
}
