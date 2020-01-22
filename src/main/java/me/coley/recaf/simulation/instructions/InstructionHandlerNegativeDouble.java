package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerNegativeDouble implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		if (ctx.pop() != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("VMTop missing");
		}
		Object v1 = ctx.pop();
		if (!(v1 instanceof Double)) {
			throw new InvalidBytecodeException("Attempted to pop double, but value was: " + v1);
		}
		ctx.push(-(Double) v1);
	}
}
