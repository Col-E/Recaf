package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.LdcInsnNode;

public final class InstructionHandlerLdc implements InstructionHandler<LdcInsnNode> {
	@Override
	public void process(LdcInsnNode instruction, ExecutionContext ctx) throws Throwable {
		ctx.push(instruction.cst);
	}
}
