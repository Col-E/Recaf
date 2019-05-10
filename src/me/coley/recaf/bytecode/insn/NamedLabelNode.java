package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Extension of LabelNode with an arbitrary opcode <i>(used internally to
 * recaf)</i> used in serialization and an identifier. The identifier is used so
 * that opcodes can wait until the entire list of opcodes is parsed so it can
 * search for the correct label with a desired identifier.
 *
 * @author Matt
 */
public class NamedLabelNode extends LabelNode {
	public static final int NAMED_LABEL = 300;
	/**
	 * Identifier.
	 */
	public final String name;

	public NamedLabelNode(String name) {
		this.opcode = NAMED_LABEL;
		this.name = name;
	}

	/**
	 * Creates a string incrementing in numerical value.
	 * Example: a, b, c, ... z, aa, ab ...
	 *
	 * @param index
	 * 		Name index.
	 *
	 * @return Generated String
	 */
	public static String generateName(int index) {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		char[] charz = alphabet.toCharArray();
		int alphabetLength = charz.length;
		int m = 8;
		final char[] array = new char[m];
		int n = m - 1;
		while(index > charz.length - 1) {
			int k = Math.abs(-(index % alphabetLength));
			array[n--] = charz[k];
			index /= alphabetLength;
			index -= 1;
		}
		array[n] = charz[index];
		return new String(array, n, m - n);
	}

	/**
	 * @param insns
	 * 		Collection of instructions.
	 *
	 * @return Map of label names to the label instances.
	 */
	public static Map<String, LabelNode> getLabels(Collection<AbstractInsnNode> insns) {
		return getLabels(insns.toArray(new AbstractInsnNode[0]));
	}

	/**
	 * @param insns
	 * 		Array of instructions.
	 *
	 * @return Map of label names to the label instances.
	 */
	public static Map<String, LabelNode> getLabels(AbstractInsnNode[] insns) {
		Map<String, LabelNode> map = new HashMap<>();
		for(AbstractInsnNode ain : insns)
			if(ain.getOpcode() == NamedLabelNode.NAMED_LABEL) {
				NamedLabelNode label = (NamedLabelNode) ain;
				map.put(label.name, label);
			}
		return map;
	}

	/**
	 * @param labels
	 * 		Map of label names to instances.
	 * @param insns
	 * 		Collection of instructions to setup named labels with the given label map.
	 */
	public static void setupLabels(Map<String, LabelNode> labels, Collection<AbstractInsnNode> insns) {
		setupLabels(labels, insns.toArray(new AbstractInsnNode[0]));
	}

	/**
	 * Populate the instructions labels, assuming they are {@link me.coley.recaf.bytecode.insn.NamedInsn} instances.
	 *
	 * @param labels
	 * 		Map of label names to instances.
	 * @param insns
	 * 		Array of instructions to setup named labels with the given label map.
	 */
	public static void setupLabels(Map<String, LabelNode> labels, AbstractInsnNode[] insns) {
		for(AbstractInsnNode ain : insns) {
			if(ain instanceof NamedInsn) {
				NamedInsn named = (NamedInsn) ain;
				named.setupLabels(labels);
			}
		}
	}

	public static InsnList clean(Map<LabelNode, LabelNode> labels, InsnList insns) {
		InsnList copy = new InsnList();
		for(AbstractInsnNode ain : insns.toArray()) {
			if(ain instanceof NamedInsn) {
				copy.add(((NamedInsn) ain).cleanClone(labels));
			} else if(ain instanceof LabelNode) {
				copy.add(labels.get(ain));
			} else {
				copy.add(ain);
			}
		}
		return copy;
	}
}