package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreLong implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Long v = ctx.popLong();
		ctx.store(instruction.var, v);
	}
}
