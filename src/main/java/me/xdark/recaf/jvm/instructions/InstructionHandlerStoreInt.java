package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerStoreInt implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int v = ctx.popInteger();
		ctx.store(instruction.var, v);
	}
}
