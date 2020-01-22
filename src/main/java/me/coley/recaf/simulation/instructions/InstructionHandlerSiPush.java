package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.IntInsnNode;

public final class InstructionHandlerSiPush implements InstructionHandler<IntInsnNode> {
	@Override
	public void process(IntInsnNode instruction, ExecutionContext ctx) throws Throwable {
		ctx.push((short) instruction.operand);
	}
}
