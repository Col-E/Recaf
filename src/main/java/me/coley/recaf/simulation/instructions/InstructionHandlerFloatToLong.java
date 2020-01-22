package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerFloatToLong implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (!(v instanceof Float)) {
			throw new InvalidBytecodeException("Attempted to load float, but value was: " + v);
		}
		ctx.push(((Float) v).longValue());
		ctx.push(VMTop.INSTANCE);
	}
}
