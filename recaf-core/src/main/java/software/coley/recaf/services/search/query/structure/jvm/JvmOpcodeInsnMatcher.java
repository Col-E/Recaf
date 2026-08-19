package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches an ASM opcode without constraining its node subtype.
 *
 * @param opcode
 * 		Opcode matcher.
 *
 * @author Matt Coley
 */
public record JvmOpcodeInsnMatcher(@Nonnull Matcher<Integer> opcode) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Opcode to match.
	 *
	 * @return Matcher against the given opcode.
	 */
	@Nonnull
	public static JvmOpcodeInsnMatcher of(int opcode) {
		return new JvmOpcodeInsnMatcher(Matchers.exact(opcode));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return StructMatchUtils.opcodeJvm(node, opcode);
	}
}
