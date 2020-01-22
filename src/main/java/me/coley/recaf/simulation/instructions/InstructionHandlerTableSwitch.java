package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import org.objectweb.asm.tree.TableSwitchInsnNode;

public final class InstructionHandlerTableSwitch implements InstructionHandler<TableSwitchInsnNode> {
	@Override
	public void process(TableSwitchInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int key = ctx.popInteger();
		if (key < instruction.min || key > instruction.max) {
			ctx.jump(instruction.dflt);
		} else {
			ctx.jump(instruction.labels.get(key - instruction.min));
		}
	}
}
