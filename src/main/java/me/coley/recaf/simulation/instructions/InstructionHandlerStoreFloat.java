package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreFloat implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Float v = ctx.popFloat();
		ctx.store(instruction.var, v);
	}
}
