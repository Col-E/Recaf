package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import me.xdark.recaf.jvm.InvalidBytecodeException;
import org.objectweb.asm.tree.IincInsnNode;

public final class InstructionHandlerIncInt implements InstructionHandler<IincInsnNode> {
	@Override
	public void process(IincInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int var = instruction.var;
		Object v = ctx.load(var);
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to load integer, but value was: " + v);
		}
		int incr = instruction.incr;
		ctx.store(var, (Integer) v + incr);
	}
}
