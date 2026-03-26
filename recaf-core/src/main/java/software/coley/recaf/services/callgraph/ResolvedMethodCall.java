package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;

/**
 * Resolved method call target.
 *
 * @param owner
 * 		Class declaring the resolved method.
 * @param method
 * 		Resolved method.
 *
 * @author Matt Coley
 */
public record ResolvedMethodCall(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {}
