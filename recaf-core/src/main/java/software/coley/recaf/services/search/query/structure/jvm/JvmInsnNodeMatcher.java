package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches a no-operand JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 *
 * @author Matt Coley
 */
public record JvmInsnNodeMatcher(@Nonnull Matcher<Integer> opcode) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Opcode to match.
	 *
	 * @return Matcher against the exact opcode.
	 */
	@Nonnull
	public static JvmInsnNodeMatcher exact(int opcode) {
		return new JvmInsnNodeMatcher(Matchers.exact(opcode));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof InsnNode && StructMatchUtils.opcodeJvm(node, opcode);
	}
}
