package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * I don't imagine this instruction getting assembler support any time soon.
 * If anyone wants to open a PR that'd be great.
 */
public class InvokeDynamic extends Assembler {
	public InvokeDynamic(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		return null;
	}
}