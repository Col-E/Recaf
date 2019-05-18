package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.AbstractAssembler;
import org.objectweb.asm.tree.*;

/**
 * I don't imagine this instruction getting assembler support any time soon.
 * If anyone wants to open a PR that'd be great.
 */
public class InvokeDynamic extends AbstractAssembler<InvokeDynamicInsnNode> {
	public InvokeDynamic(int opcode) {super(opcode);}

	@Override
	public InvokeDynamicInsnNode parse(String text) {
		return null;
	}

	@Override
	public String generate(MethodNode method, InvokeDynamicInsnNode insn) {
		return null;
	}
}