package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

/**
 * Pair of a name and type.
 *
 * @param name
 * 		Element name.
 * @param type
 * 		Element type.
 */
public record NameTypePair(@Nonnull String name, @Nonnull String type) {}
