package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerLoadInt implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.load(instruction.var);
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Attempted to load integer, but value was: " + v);
		}
		ctx.push(v);
	}
}
