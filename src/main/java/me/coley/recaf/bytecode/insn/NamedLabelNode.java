package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

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
}