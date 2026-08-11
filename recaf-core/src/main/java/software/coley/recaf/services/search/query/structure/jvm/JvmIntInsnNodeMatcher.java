package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches an integer-operand JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param operand
 * 		Integer operand matcher.
 *
 * @author Matt Coley
 */
public record JvmIntInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                    @Nonnull Matcher<Integer> operand) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Opcode to match.
	 * @param operand
	 * 		Operand to match.
	 *
	 * @return Matcher against the exact opcode and operand.
	 */
	@Nonnull
	public static JvmIntInsnNodeMatcher exact(int opcode, int operand) {
		return new JvmIntInsnNodeMatcher(Matchers.exact(opcode), Matchers.exact(operand));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof IntInsnNode value && StructMatchUtils.opcodeJvm(node, opcode) && operand.matches(value.operand);
	}
}
