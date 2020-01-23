package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.JumpInsnNode;

public final class InstructionHandlerIntsGreaterEquals implements InstructionHandler<JumpInsnNode> {
	@Override
	public void process(JumpInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int v1 = ctx.popInteger();
		int v = ctx.popInteger();
		if (v >= v1) {
			ctx.jump(instruction.label);
		}
	}
}
