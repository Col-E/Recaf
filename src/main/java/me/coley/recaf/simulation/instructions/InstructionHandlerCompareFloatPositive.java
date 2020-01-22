package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerCompareFloatPositive implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Float v1 = ctx.popFloat();
		Float v = ctx.popFloat();
		if (Float.isNaN(v) || Float.isNaN(v1)) {
			ctx.push(1);
			return;
		}
		float result = v - v1;
		if (result == 0) {
			ctx.push(0);
		} else {
			ctx.push(result > 0 ? 1 : -1);
		}
	}
}
