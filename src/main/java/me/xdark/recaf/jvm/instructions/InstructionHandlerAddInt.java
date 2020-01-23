package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerAddInt implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int v1 = ctx.popInteger();
		int v2 = ctx.popInteger();
		ctx.push( v1 +  v2);
	}
}
