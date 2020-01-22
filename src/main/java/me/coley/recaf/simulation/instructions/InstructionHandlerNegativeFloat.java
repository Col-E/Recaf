package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerNegativeFloat implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v1 = ctx.pop();
		if (!(v1 instanceof Float)) {
			throw new InvalidBytecodeException("Attempted to pop float, but value was: " + v1);
		}
		ctx.push(-(Float) v1);
	}
}
