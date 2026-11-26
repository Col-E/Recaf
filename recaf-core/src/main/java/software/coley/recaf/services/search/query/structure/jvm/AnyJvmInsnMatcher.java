package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Matches every JVM instruction node.
 *
 * @author Matt Coley
 */
public record AnyJvmInsnMatcher() implements JvmInsnMatcher {
	/** Singleton instance. */
	public static final AnyJvmInsnMatcher INSTANCE = new AnyJvmInsnMatcher();

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return true;
	}
}
