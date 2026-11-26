package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Pair of a class and field.
 *
 * @param classNode
 * 		Wrapped class.
 * @param fieldNode
 * 		Wrapped field.
 */
public record ClassFieldPair(@Nonnull ClassNode classNode,
                             @Nonnull FieldNode fieldNode) {}