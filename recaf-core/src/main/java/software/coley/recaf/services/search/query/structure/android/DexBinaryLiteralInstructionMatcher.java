package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX binary arithmetic or bitwise instruction with an inline literal.
 *
 * @param opcode
 * 		Binary-literal opcode matcher. This can distinguish the {@code lit8} and {@code lit16} encodings.
 * @param dest
 * 		Destination register matcher.
 * @param src
 * 		Source register matcher.
 * @param constant
 * 		Sign-extended literal matcher.
 *
 * @author Matt Coley
 */
public record DexBinaryLiteralInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                 @Nonnull Matcher<Integer> dest,
                                                 @Nonnull Matcher<Integer> src,
                                                 @Nonnull Matcher<Integer> constant) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact binary-literal opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param src
	 * 		Exact source register.
	 * @param constant
	 * 		Exact sign-extended literal.
	 *
	 * @return Matcher against the exact binary-literal instruction.
	 */
	@Nonnull
	public static DexBinaryLiteralInstructionMatcher exact(int opcode, int dest, int src, int constant) {
		return new DexBinaryLiteralInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest),
				Matchers.exact(src), Matchers.exact(constant));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof BinaryLiteralInstruction value
				&& opcode.matches(value.opcode())
				&& dest.matches(value.dest())
				&& src.matches(value.src())
				&& constant.matches(value.constant());
	}
}
