package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;

/**
 * Unresolved method call. Information kept in case workspace changes allow resolving the call later.
 *
 * @param callingClass
 * 		The class that is making the method call.
 * @param callingMethod
 * 		The method that is making the method call.
 * @param callSite
 * 		The unresolved call site. Contains the method reference of the callee and the invocation kind.
 *
 * @author Matt Coley
 */
public record UnresolvedCall(@Nonnull ClassInfo callingClass,
                             @Nonnull MethodRef callingMethod,
                             @Nonnull CallSite callSite) {}
