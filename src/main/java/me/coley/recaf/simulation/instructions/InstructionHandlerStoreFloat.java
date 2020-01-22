package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreFloat implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (!(v instanceof Float)) {
			throw new InvalidBytecodeException("Attempted to store float, but value was: " + v);
		}
		ctx.store(instruction.var, v);
	}
}
