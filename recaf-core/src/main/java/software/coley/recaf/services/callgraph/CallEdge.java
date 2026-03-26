package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;

/**
 * Edge in the call graph.
 *
 * @param caller
 * 		Caller method vertex.
 * @param callee
 * 		Callee method vertex.
 * @param callSite
 * 		Call site details of the call.
 *
 * @author Matt Coley
 */
public record CallEdge(@Nonnull MutableMethodVertex caller,
                       @Nonnull MutableMethodVertex callee,
                       @Nonnull CallSite callSite) {}
