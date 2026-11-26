package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Pair of a class and field.
 *
 * @param classNode
 * 		Wrapped class.
 * @param methodNode
 * 		Wrapped method.
 */
public record ClassMethodPair(@Nonnull ClassNode classNode,
                              @Nonnull MethodNode methodNode) {
	@Nonnull
	@Override
	public String toString() {
		return classNode.name + "." + methodNode.name + methodNode.desc;
	}
}