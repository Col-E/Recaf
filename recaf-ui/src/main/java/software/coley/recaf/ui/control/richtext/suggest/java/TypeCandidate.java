package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;

/**
 * Lightweight description of a type that can participate in Java completion lookups.
 *
 * @param simpleName
 * 		Display-friendly short name.
 * @param qualifiedName
 * 		Java source style <i>(dot-separated)</i> name.
 * @param internalName
 * 		Internal class name.
 * @param packageName
 * 		Package containing the type.
 * @param annotation
 * 		Flag indicating the type is an annotation.
 * @param access
 * 		Raw access flags from the backing class.
 * @param path
 * 		Workspace path to the class, or {@code null} if the type is not backed by a workspace entry.
 *
 * @author Matt Coley
 */
public record TypeCandidate(@Nonnull String simpleName,
                            @Nonnull String qualifiedName,
                            @Nonnull String internalName,
                            @Nonnull String packageName,
                            boolean annotation,
                            int access,
                            @Nullable ClassPathNode path) {}
