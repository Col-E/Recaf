package me.coley.recaf.simulation;

import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public final class ExecutionContext {
	private final Workspace workspace;
	private final ExecutionStack stack;
	private final Object[] locals;
	private final InsnList instructions;
	private int cursor;
	private boolean run;

	public ExecutionContext(Workspace workspace, MethodNode method) {
		this.workspace = workspace;
		this.stack = new ExecutionStack(method.maxStack);
		this.locals = new Object[method.maxLocals];
		this.instructions = method.instructions;
	}

	public Object run() {
		run = true;
		return execute();
	}

	public Object execute() {
		throw new UnsupportedOperationException();
	}

	public void push(Object v) {
		stack.push(v);
	}

	public <V> V pop() {
		return stack.pop();
	}
}
