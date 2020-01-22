package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerNegativeLong implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		if (ctx.pop() != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("VMTop missing");
		}
		Object v1 = ctx.pop();
		if (!(v1 instanceof Long)) {
			throw new InvalidBytecodeException("Attempted to pop long, but value was: " + v1);
		}
		ctx.push(-(Long) v1);
	}
}
