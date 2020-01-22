package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerReturnLong implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Long result = ctx.popLong();
		ctx.complete(result);
	}
}
