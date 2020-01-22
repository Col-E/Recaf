package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerCompareDoublePositive implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Double v1 = ctx.popDouble();
		Double v = ctx.popDouble();
		if (Double.isNaN(v) || Double.isNaN(v1)) {
			ctx.push(1);
			return;
		}
		double result = v - v1;
		if (result == 0) {
			ctx.push(0);
		} else {
			ctx.push(result > 0 ? 1 : -1);
		}
	}
}
