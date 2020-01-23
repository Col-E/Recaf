package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.JumpInsnNode;

public final class InstructionHandlerIntLessZero implements InstructionHandler<JumpInsnNode> {
	@Override
	public void process(JumpInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int v = ctx.popInteger();
		if (v < 0) {
			ctx.jump(instruction.label);
		}
	}
}
