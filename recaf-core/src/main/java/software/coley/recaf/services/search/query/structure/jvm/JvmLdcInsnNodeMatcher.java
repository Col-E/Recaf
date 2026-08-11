package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a JVM constant-load instruction.
 *
 * @param constant
 * 		Constant matcher.
 *
 * @author Matt Coley
 */
public record JvmLdcInsnNodeMatcher(@Nonnull Matcher<Object> constant) implements JvmInsnMatcher {
	/**
	 * @param constant
	 * 		Constant to match.
	 *
	 * @return Matcher against the exact constant value.
	 */
	@Nonnull
	public static JvmLdcInsnNodeMatcher exact(@Nonnull Object constant) {
		return new JvmLdcInsnNodeMatcher(Matchers.exact(constant));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof LdcInsnNode value
				&& constant.matches(value.cst);
	}
}
