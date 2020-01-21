package me.coley.recaf.simulation;

public final class ExecutionStack {
	private final Object[] stack;
	private int cursor;

	public ExecutionStack(int maxSize) {
		this.stack = new Object[maxSize];
	}

	public void push(Object v) {
		int next = cursor++;
		if (next == stack.length) {
			throw new ArrayIndexOutOfBoundsException(next);
		}
		stack[next] = v;
	}

	public <V> V pop() {
		int prev = cursor--;
		if (prev == -1) {
			throw new ArrayIndexOutOfBoundsException(prev);
		}
		V v = (V) stack[prev];
		stack[prev] = null;
		return v;
	}
}
