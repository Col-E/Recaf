package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.JumpInsnNode;

public final class InstructionHandlerReturnSubroutine implements InstructionHandler<JumpInsnNode> {
	@Override
	public void process(JumpInsnNode instruction, ExecutionContext ctx) throws Throwable {
	}
}
