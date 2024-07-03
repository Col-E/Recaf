package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;

/**
 * Basic method outline.
 *
 * @param owner
 * 		Method reference owner.
 * @param name
 * 		Method name.
 * @param desc
 * 		Method descriptor.
 *
 * @author Amejonah
 */
public record MethodRef(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {}
