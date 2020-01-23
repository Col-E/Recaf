package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerDupX2 implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v1 = ctx.pop(), v2 = ctx.pop();
		ctx.push(v1);
		ctx.push(v2);
		ctx.push(v1);
		ctx.push(v2);
	}
}
