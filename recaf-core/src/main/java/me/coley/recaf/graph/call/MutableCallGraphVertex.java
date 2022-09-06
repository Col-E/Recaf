package me.coley.recaf.graph.call;

import me.coley.recaf.code.MethodInfo;

import java.util.*;

public final class MutableCallGraphVertex implements CallGraphVertex {
	private final Set<CallGraphVertex> callers = Collections.newSetFromMap(new LinkedHashMap<>());
	private final Set<CallGraphVertex> calls = Collections.newSetFromMap(new LinkedHashMap<>());
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
