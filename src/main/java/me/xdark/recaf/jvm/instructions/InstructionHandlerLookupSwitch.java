package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;

public final class InstructionHandlerLookupSwitch implements InstructionHandler<LookupSwitchInsnNode> {
	@Override
	public void process(LookupSwitchInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int key = ctx.popInteger();
		int index = instruction.keys.indexOf(key);
		LabelNode node = index == -1 ? instruction.dflt : instruction.labels.get(index);
		ctx.jump(node);
	}
}
