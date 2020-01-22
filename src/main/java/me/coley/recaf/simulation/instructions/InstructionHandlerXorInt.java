package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerXorInt implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v1 = ctx.pop();
		if (!(v1 instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to pop integer, but value was: " + v1);
		}
		Object v2 = ctx.pop();
		if (!(v2 instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to pop integer, but value was: " + v2);
		}
		ctx.push((Integer) v1 ^ (Integer) v2);
	}
}
