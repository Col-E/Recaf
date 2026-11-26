package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import software.coley.recaf.services.search.match.Matcher;

/**
 * Matches a JVM multi-dimensional array allocation.
 *
 * @param descriptor
 * 		Array descriptor matcher.
 * @param dimensions
 * 		Dimension count matcher.
 *
 * @author Matt Coley
 */
public record JvmMultiANewArrayInsnNodeMatcher(@Nonnull Matcher<String> descriptor,
                                               @Nonnull Matcher<Integer> dimensions) implements JvmInsnMatcher {
	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof MultiANewArrayInsnNode value
				&& descriptor.matches(value.desc)
				&& dimensions.matches(value.dims);
	}
}
