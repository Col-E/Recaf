package me.coley.recaf.graph.call;

import me.coley.recaf.code.MemberSignature;
import me.coley.recaf.code.MethodInfo;

import javax.annotation.Nonnull;
import java.util.*;

public final class MutableCallGraphVertex implements CallGraphVertex {
	private final Set<CallGraphVertex> callers = new HashSet<>();
	private final Set<CallGraphVertex> calls = Collections.newSetFromMap(new LinkedHashMap<>());
	private final MethodInfo methodInfo;
	boolean visited;

	public MutableCallGraphVertex(@Nonnull MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
	}

	@Nonnull
	@Override
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Nonnull
	@Override
	public MemberSignature getSignature() {
		return methodInfo.getMemberSignature();
	}

	@Override
	public Collection<CallGraphVertex> getCallers() {
		return callers;
	}

	@Override
	public Collection<CallGraphVertex> getCalls() {
		return calls;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MutableCallGraphVertex that = (MutableCallGraphVertex) o;

		return methodInfo.equals(that.methodInfo);
	}

	@Override
	public int hashCode() {
		return methodInfo.hashCode();
	}
}
