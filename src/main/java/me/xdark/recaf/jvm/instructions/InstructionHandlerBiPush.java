package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.IntInsnNode;

public final class InstructionHandlerBiPush implements InstructionHandler<IntInsnNode> {
	@Override
	public void process(IntInsnNode instruction, ExecutionContext ctx) throws Throwable {
		ctx.push((byte) instruction.operand);
	}
}
