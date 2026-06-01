package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;

/**
 * Unit of a single class source to compile.
 *
 * @param className
 * 		Internal name of the class defined in the source.
 * @param source
 * 		Source model including the original input source, augmented source, and source mappings between the two.
 */
public record AugmentedSourceUnit(@Nonnull String className,
                                  @Nonnull AugmentedSource source) {}
