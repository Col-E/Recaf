package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;

/**
 * Common representation of a method call site.
 *
 * @param owner
 * 		Called method owner.
 * @param name
 * 		Called method name.
 * @param descriptor
 * 		Called method descriptor.
 * @param kind
 * 		Invocation kind.
 * @param interfaceCall
 * 		Interface dispatch flag used by JVM call resolution.
 *
 * @author Matt Coley
 */
public record CallSite(@Nonnull String owner,
                       @Nonnull String name,
                       @Nonnull String descriptor,
                       @Nonnull InvokeKind kind,
                       boolean interfaceCall) {
	/**
	 * @return Method reference of the callee.
	 */
	@Nonnull
	public MethodRef methodRef() {
		return new MethodRef(owner, name, descriptor);
	}
}
