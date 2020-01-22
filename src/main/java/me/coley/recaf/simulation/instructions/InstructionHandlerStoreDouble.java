package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreDouble implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (v != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("VMTop missing");
		}
		if (!((v = ctx.pop()) instanceof Double)) {
			throw new InvalidBytecodeException("Attempted to store double, but value was: " + v);
		}
		ctx.store(instruction.var, v);
	}
}
