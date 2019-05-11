package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.AbstractAssembler;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

/**
 * Insn assembler
 *
 * <pre>
 *     n/a - no args
 * </pre>
 *
 * @author Matt
 */
public class Insn extends AbstractAssembler {
	public Insn(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		return new InsnNode(opcode);
	}
}