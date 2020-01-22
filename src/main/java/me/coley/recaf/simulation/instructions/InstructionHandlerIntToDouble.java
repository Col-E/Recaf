package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerIntToDouble implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to pop integer, but value was: " + v);
		}
		ctx.push(((Integer) v).doubleValue());
		ctx.push(VMTop.INSTANCE);
	}
}
