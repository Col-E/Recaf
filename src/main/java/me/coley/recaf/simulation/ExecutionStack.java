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
		stack[cursor++] = v;
	}

	<V> V pop() {
		int prev = --cursor;
		V v = (V) stack[prev];
		stack[prev] = null;
		return v;
	}
}
