package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import me.xdark.recaf.jvm.InvalidBytecodeException;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class InstructionHandlerIntToShort implements InstructionHandler<AbstractInsnNode> {
	@Override
	public void process(AbstractInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.pop();
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to pop integer, but value was: " + v);
		}
		ctx.push(((Integer) v).shortValue());
	}
}
