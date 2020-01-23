package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.LdcInsnNode;

public final class InstructionHandlerLdc implements InstructionHandler<LdcInsnNode> {
	@Override
	public void process(LdcInsnNode instruction, ExecutionContext ctx) throws Throwable {
		ctx.push(instruction.cst);
	}
}
