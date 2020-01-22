package me.coley.recaf.simulation.instructions;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.InstructionHandler;
import me.coley.recaf.simulation.InvalidBytecodeException;
import me.coley.recaf.simulation.VMTop;
import org.objectweb.asm.tree.VarInsnNode;

public final class InstructionHandlerLoadDouble implements InstructionHandler<VarInsnNode> {
	@Override
	public void process(VarInsnNode instruction, ExecutionContext ctx) throws Throwable {
		Object v = ctx.load(instruction.var);
		if (!(v instanceof Double)) {
			throw new InvalidBytecodeException("Attempted to load double, but value was: " + v);
		}
		ctx.push(v);
		ctx.push(VMTop.INSTANCE);
	}
}
