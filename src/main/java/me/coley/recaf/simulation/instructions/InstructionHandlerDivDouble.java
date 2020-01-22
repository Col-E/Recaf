package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerDivDouble implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		if (ctx.pop() != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("VMTop missing");
		}
		Object v1 = ctx.pop();
		if (!(v1 instanceof Double)) {
			throw new InvalidBytecodeException("Attempted to pop double, but value was: " + v1);
		}
		if (ctx.pop() != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("VMTop missing");
		}
		Object v2 = ctx.pop();
		if (!(v2 instanceof Double)) {
			throw new InvalidBytecodeException("Attempted to pop double, but value was: " + v2);
		}
		ctx.push((Double) v1 / (Double) v2);
	}
}
