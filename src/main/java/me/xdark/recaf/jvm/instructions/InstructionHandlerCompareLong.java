package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerCompareLong implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Long v1 = ctx.popLong();
		Long v = ctx.popLong();
		long result = v - v1;
		if (result == 0) {
			ctx.push(0);
		} else {
			ctx.push(result > 0 ? 1 : -1);
		}
	}
}
