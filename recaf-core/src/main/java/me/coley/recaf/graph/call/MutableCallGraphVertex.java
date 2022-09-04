package me.coley.recaf.graph.call;

import me.coley.recaf.code.MethodInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class MutableCallGraphVertex implements CallGraphVertex {
	private final Set<CallGraphVertex> callers = new HashSet<>();
	private final Set<CallGraphVertex> calls = new HashSet<>();
	private final MethodInfo methodInfo;
	boolean visited;

	public MutableCallGraphVertex(MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
	}

	@Override
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Override
	public Collection<CallGraphVertex> getCallers() {
		return callers;
	}

	@Override
	public Collection<CallGraphVertex> getCalls() {
		return calls;
	}
}
