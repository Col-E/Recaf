package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a JVM local-variable increment.
 *
 * @param variable
 * 		Local variable index matcher.
 * @param increment
 * 		Increment amount matcher.
 *
 * @author Matt Coley
 */
public record JvmIincInsnNodeMatcher(@Nonnull Matcher<Integer> variable,
                                     @Nonnull Matcher<Integer> increment) implements JvmInsnMatcher {
	/**
	 * @param variable
	 * 		Variable index to match.
	 * @param increment
	 * 		Increment amount to match.
	 *
	 * @return Matcher against the exact variable and increment values.
	 */
	@Nonnull
	public static JvmIincInsnNodeMatcher exact(int variable, int increment) {
		return new JvmIincInsnNodeMatcher(Matchers.exact(variable), Matchers.exact(increment));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof IincInsnNode value && variable.matches(value.var) && increment.matches(value.incr);
	}
}
