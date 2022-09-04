package me.coley.recaf.graph.call;

import me.coley.recaf.code.MethodInfo;

import java.util.Collection;

public interface CallGraphVertex {

	MethodInfo getMethodInfo();

	Collection<CallGraphVertex> getCallers();

	Collection<CallGraphVertex> getCalls();
}
