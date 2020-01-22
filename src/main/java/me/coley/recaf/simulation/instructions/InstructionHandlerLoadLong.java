package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerLoadLong implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.load(instruction.var);
		if (!(v instanceof Long)) {
			throw new InvalidBytecodeException("Attempted to load long, but value was: " + v);
		}
		ctx.pushTop(v);
	}
}
