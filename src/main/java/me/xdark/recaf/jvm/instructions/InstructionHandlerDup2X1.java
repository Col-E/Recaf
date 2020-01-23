package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerDup2X1 implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v1 = ctx.pop();
		Object v2 = ctx.pop();
		Object v3 = ctx.pop();
		ctx.push(v2);
		ctx.push(v1);
		ctx.push(v3);
		ctx.push(v2);
		ctx.push(v1);
	}
}
