package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX three-register binary arithmetic or bitwise instruction.
 *
 * @param opcode
 * 		Binary opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param a
 * 		First source register matcher.
 * @param b
 * 		Second source register matcher.
 *
 * @author Matt Coley
 */
public record DexBinaryInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                          @Nonnull Matcher<Integer> dest,
                                          @Nonnull Matcher<Integer> a,
                                          @Nonnull Matcher<Integer> b) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact binary opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param a
	 * 		Exact first source register.
	 * @param b
	 * 		Exact second source register.
	 *
	 * @return Matcher against the exact binary instruction.
	 */
	@Nonnull
	public static DexBinaryInstructionMatcher exact(int opcode, int dest, int a, int b) {
		return new DexBinaryInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest),
				Matchers.exact(a), Matchers.exact(b));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof BinaryInstruction value
				&& opcode.matches(value.opcode())
				&& dest.matches(value.dest())
				&& a.matches(value.a())
				&& b.matches(value.b());
	}
}
