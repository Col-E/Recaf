package me.coley.recaf.ui.component;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * HTextBox that stores the opcode represented in the text.
 * 
 * @author Matt
 */
public class HOpcodeBox extends HTextBox {
	private final AbstractInsnNode insn;

	public HOpcodeBox(AbstractInsnNode insn) {
		this.insn = insn;
	}

	public AbstractInsnNode getInsn() {
		return insn;
	}
}
