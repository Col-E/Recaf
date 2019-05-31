package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import org.objectweb.asm.tree.*;

/**
 * Insn assembler
 *
 * <pre>
 *     n/a - no args
 * </pre>
 *
 * @author Matt
 */
public class Insn extends AbstractAssembler<InsnNode> {
	public Insn(int opcode) {super(opcode);}

	@Override
	public InsnNode parse(String text) {
		return new InsnNode(opcode);
	}

	@Override
	public String generate(MethodNode method, InsnNode insn) {
		return OpcodeUtil.opcodeToName(opcode);
	}
}