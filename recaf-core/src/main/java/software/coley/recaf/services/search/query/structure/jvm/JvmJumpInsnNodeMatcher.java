package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches a conditional or unconditional JVM branch.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param label
 * 		Branch target matcher.
 *
 * @author Matt Coley
 */
public record JvmJumpInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                     @Nonnull Matcher<Label> label) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Opcode to match.
	 *
	 * @return Matcher against the exact opcode.
	 */
	@Nonnull
	public static JvmJumpInsnNodeMatcher op(int opcode) {
		return new JvmJumpInsnNodeMatcher(Matchers.exact(opcode), Matchers.any());
	}

	/**
	 * @return Matcher that matches any jump instruction.
	 */
	@Nonnull
	public static JvmJumpInsnNodeMatcher any() {
		return new JvmJumpInsnNodeMatcher(Matchers.any(), Matchers.any());
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof JumpInsnNode value
				&& StructMatchUtils.opcodeJvm(node, opcode)
				&& label.matches(value.label.getLabel());
	}
}
