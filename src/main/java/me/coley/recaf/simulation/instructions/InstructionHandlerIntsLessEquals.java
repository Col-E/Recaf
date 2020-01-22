package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.util.InsnUtil;
import org.objectweb.asm.tree.JumpInsnNode;

public final class InstructionHandlerIntsLessEquals implements InstructionHandler<JumpInsnNode> {
	@Override
	public void process(JumpInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Integer v1 = ctx.popInteger();
		Integer v = ctx.popInteger();
		if (v <= v1) {
			ctx.jump(instruction.label);
		}
	}
}
