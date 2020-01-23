package me.xdark.recaf.jvm;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionHandler<I extends AbstractInsnNode> {
	void process(I instruction, ExecutionContext ctx) throws Throwable;
}
