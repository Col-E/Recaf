package me.coley.recaf.graph.call;

import me.coley.recaf.code.MemberSignature;
import me.coley.recaf.code.MethodInfo;

import javax.annotation.Nonnull;
import java.util.*;

public final class MutableCallGraphVertex implements CallGraphVertex {
	private final Set<CallGraphVertex> callers = Collections.newSetFromMap(new LinkedHashMap<>());
	private final Set<CallGraphVertex> calls = Collections.newSetFromMap(new LinkedHashMap<>());
	MethodInfo methodInfo;
	private MemberSignature signature;
	boolean visited;

	public MutableCallGraphVertex(@Nonnull MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
	}

	public MutableCallGraphVertex(@Nonnull MemberSignature signature) {
		this.signature = signature;
	}

	public void setMethodInfo(MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
		this.signature = methodInfo.getMemberSignature();
	}

	@Override
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Nonnull
	@Override
	public MemberSignature getSignature() {
		return signature;
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
