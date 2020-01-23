package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import me.xdark.recaf.jvm.InvalidBytecodeException;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Array;

public final class InstructionHandlerLoadArrayLong implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int index = instruction.var;
		Object array = ctx.pop();
		if (!array.getClass().isArray()) {
			throw new InvalidBytecodeException("Object is not an array");
		}
		Object v = Array.get(array, index);
		if (!(v instanceof Long)) {
			throw new InvalidBytecodeException("Attempted to load long, but value was: " + v);
		}
		ctx.pushTop(v);
	}
}
