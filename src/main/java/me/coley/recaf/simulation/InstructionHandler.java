package me.coley.recaf.simulation;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionHandler<I extends AbstractInsnNode> {
	void process(I instruction, ExecutionContext ctx) throws Throwable;

	default Long popLong(ExecutionContext ctx) throws SimulationException {
		Object top = ctx.pop();
		if (top != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("Expected VMTop, but got: " + top);
		}
		Object v = ctx.pop();
		if (!(v instanceof Long)) {
			throw new InvalidBytecodeException("Expected to pop long, but got: " + v);
		}
		return (Long) v;
	}

	default Double popDouble(ExecutionContext ctx) throws SimulationException {
		Object top = ctx.pop();
		if (top != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("Expected VMTop, but got: " + top);
		}
		Object v = ctx.pop();
		if (!(v instanceof Double)) {
			throw new InvalidBytecodeException("Expected to pop double, but got: " + v);
		}
		return (Double) v;
	}

	default Integer popInteger(ExecutionContext ctx) throws SimulationException {
		Object top = ctx.pop();
		if (top != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("Expected VMTop, but got: " + top);
		}
		Object v = ctx.pop();
		if (!(v instanceof Integer)) {
			throw new InvalidBytecodeException("Expected to pop int, but got: " + v);
		}
		return (Integer) v;
	}
}
