package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link MethodVertex}.
 *
 * @author Matt Coley
 */
public class MutableMethodVertex implements MethodVertex {
	private final Set<MethodVertex> callers = Collections.synchronizedSet(new HashSet<>());
	private final Set<MethodVertex> calls = Collections.synchronizedSet(new HashSet<>());
	private final Set<CallEdge> incomingEdges = Collections.synchronizedSet(new HashSet<>());
	private final Set<CallEdge> outgoingEdges = Collections.synchronizedSet(new HashSet<>());
	private final MethodRef method;
	private final MethodMember resolvedMethod;

	/**
	 * @param method
	 * 		Wrapped method reference.
	 * @param resolvedMethod
	 * 		Resolved method declaration.
	 */
	public MutableMethodVertex(@Nonnull MethodRef method, @Nonnull MethodMember resolvedMethod) {
		this.method = method;
		this.resolvedMethod = resolvedMethod;
	}

	/**
	 * Adds a call edge from this method vertex to the given callee.
	 *
	 * @param callee
	 * 		Method vertex being called.
	 * @param callSite
	 * 		Call site details of the call.
	 */
	public void addCall(@Nonnull MutableMethodVertex callee, @Nonnull CallSite callSite) {
		CallEdge edge = new CallEdge(this, callee, callSite);
		boolean linked;
		synchronized (outgoingEdges) {
			linked = outgoingEdges.add(edge);
		}
		synchronized (callee.incomingEdges) {
			linked |= callee.incomingEdges.add(edge);
		}
		if (linked) {
			calls.add(callee);
			callee.callers.add(this);
		}
	}

	/**
	 * @return Snapshot of incoming edges.
	 */
	@Nonnull
	private Collection<CallEdge> getIncomingEdgesSnapshot() {
		synchronized (incomingEdges) {
			return new HashSet<>(incomingEdges);
		}
	}

	/**
	 * Removes this method vertex from all connected vertices.
	 */
	public void prune() {
		for (CallEdge edge : getOutgoingEdgesSnapshot())
			edge.callee().removeIncomingEdge(edge);
		for (CallEdge edge : getIncomingEdgesSnapshot())
			edge.caller().removeOutgoingEdge(edge);
		synchronized (outgoingEdges) {
			outgoingEdges.clear();
		}
		synchronized (incomingEdges) {
			incomingEdges.clear();
		}
		calls.clear();
		callers.clear();
	}

	private void removeIncomingEdge(@Nonnull CallEdge edge) {
		synchronized (incomingEdges) {
			if (!incomingEdges.remove(edge))
				return;
			if (incomingEdges.stream().noneMatch(other -> other.caller() == edge.caller()))
				callers.remove(edge.caller());
		}
	}

	private void removeOutgoingEdge(@Nonnull CallEdge edge) {
		synchronized (outgoingEdges) {
			if (!outgoingEdges.remove(edge))
				return;
			if (outgoingEdges.stream().noneMatch(other -> other.callee() == edge.callee()))
				calls.remove(edge.callee());
		}
	}

	/**
	 * @return Snapshot of outgoing edges.
	 */
	@Nonnull
	private Collection<CallEdge> getOutgoingEdgesSnapshot() {
		synchronized (outgoingEdges) {
			return new HashSet<>(outgoingEdges);
		}
	}

	@Nonnull
	@Override
	public MethodRef getMethod() {
		return method;
	}

	@Nonnull
	@Override
	public MethodMember getResolvedMethod() {
		return resolvedMethod;
	}

	@Nonnull
	@Override
	public Collection<MethodVertex> getCallers() {
		return callers;
	}

	@Nonnull
	@Override
	public Collection<MethodVertex> getCalls() {
		return calls;
	}

	@Nonnull
	@Override
	public Collection<CallEdge> getCallerEdges() {
		return getIncomingEdgesSnapshot();
	}

	@Nonnull
	@Override
	public Collection<CallEdge> getCallEdges() {
		return getOutgoingEdgesSnapshot();
	}

	@Override
	public String toString() {
		return method.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MutableMethodVertex vertex = (MutableMethodVertex) o;
		return method.equals(vertex.method);
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}
}
