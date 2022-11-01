package me.coley.recaf.graph.call;

import me.coley.recaf.code.MemberSignature;
import me.coley.recaf.code.MethodInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface CallGraphVertex {

	/**
	 * @return MethodInfo of this vertex, it's null when it's not resolved: a method which isn't in available in the workspace.
	 */
	@Nullable
	MethodInfo getMethodInfo();

	@Nonnull
	MemberSignature getSignature();

	Collection<CallGraphVertex> getCallers();

	Collection<CallGraphVertex> getCalls();

}
