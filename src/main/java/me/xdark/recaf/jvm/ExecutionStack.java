package me.xdark.recaf.jvm;

import java.util.Arrays;

final class ExecutionStack {
	private final java.lang.Object[] stack;
	private int cursor;

	ExecutionStack(int maxSize) {
		this.stack = new java.lang.Object[maxSize];
	}

	void clear() {
		Arrays.fill(stack, null);
		cursor = 0;
	}

	void push(java.lang.Object v) {
		stack[cursor++] = v;
	}

	<V> V pop() {
		int prev = --cursor;
		V v = (V) stack[prev];
		stack[prev] = null;
		return v;
	}

	boolean isEmpty() {
		return cursor == 0;
	}
}
