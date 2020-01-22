package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerIntToChar implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to pop integer, but value was: " + v);
		}
		ctx.push((char) ((Integer) v).intValue());
	}
}
