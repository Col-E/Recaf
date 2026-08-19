package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.jvm.JvmOpcodeInsnMatcher;

/**
 * Matches a dex opcode without constraining its instruction subtype.
 *
 * @param opcode
 * 		Opcode matcher.
 *
 * @author Matt Coley
 */
public record DexOpcodeInsnMatcher(@Nonnull Matcher<Integer> opcode) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Opcode to match.
	 *
	 * @return Matcher against the given opcode.
	 */
	@Nonnull
	public static DexOpcodeInsnMatcher exact(int opcode) {
		return new DexOpcodeInsnMatcher(Matchers.exact(opcode));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return opcode.matches(instruction.opcode());
	}
}
