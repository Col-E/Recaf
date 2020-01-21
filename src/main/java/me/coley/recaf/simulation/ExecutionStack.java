package me.coley.recaf.simulation;

import java.util.Arrays;

final class ExecutionStack {
	private final Object[] stack;
	private int cursor;

	ExecutionStack(int maxSize) {
		this.stack = new Object[maxSize];
	}

	void clear() {
		Arrays.fill(stack, null);
		cursor = 0;
	}

	void push(Object v) {
		int next = cursor++;
		if (next == stack.length) {
			throw new ArrayIndexOutOfBoundsException(next);
		}
		stack[next] = v;
	}

	<V> V pop() {
		int prev = cursor--;
		if (prev == -1) {
			throw new ArrayIndexOutOfBoundsException(prev);
		}
		V v = (V) stack[prev];
		stack[prev] = null;
		return v;
	}
}
