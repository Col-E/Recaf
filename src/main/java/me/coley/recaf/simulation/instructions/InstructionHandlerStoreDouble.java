package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreDouble implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Double v = ctx.popDouble();
		ctx.store(instruction.var, v);
	}
}
