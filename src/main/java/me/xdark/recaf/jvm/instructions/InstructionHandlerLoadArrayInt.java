package me.xdark.recaf.jvm.instructions;

import me.xdark.recaf.jvm.ExecutionContext;
import me.xdark.recaf.jvm.InstructionHandler;
import me.xdark.recaf.jvm.InvalidBytecodeException;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Array;

public final class InstructionHandlerLoadArrayInt implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		int index = instruction.var;
		Object array = ctx.pop();
		if (!array.getClass().isArray()) {
			throw new InvalidBytecodeException("Object is not an array");
		}
		Object v = Array.get(array, index);
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to load integer, but value was: " + v);
		}
		ctx.push(v);
	}
}
