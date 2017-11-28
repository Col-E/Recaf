package me.coley.recaf.asm.opcode;

import org.objectweb.asm.tree.LabelNode;

/**
 * Extension of LabelNode with an arbitrary opcode used in serialization and an
 * identifier. The identifier is used so that opcodes can wait until the entire
 * list of opcodes is parsed so it can search for the correct label with a
 * desired identifier.
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
}