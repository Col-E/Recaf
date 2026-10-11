package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.MoveInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code move} instruction and its register operands.
 *
 * @param opcode
 * 		Opcode matcher. This can distinguish the compact, {@code /from16}, and {@code /16} encodings.
 * @param to
 * 		Destination register matcher.
 * @param from
 * 		Source register matcher.
 *
 * @author Matt Coley
 */
public record DexMoveInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                        @Nonnull Matcher<Integer> to,
                                        @Nonnull Matcher<Integer> from) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact move opcode.
	 * @param to
	 * 		Exact destination register.
	 * @param from
	 * 		Exact source register.
	 *
	 * @return Matcher against the exact move instruction.
	 */
	@Nonnull
	public static DexMoveInstructionMatcher exact(int opcode, int to, int from) {
		return new DexMoveInstructionMatcher(Matchers.exact(opcode), Matchers.exact(to), Matchers.exact(from));
	}

	/**
	 * @param to
	 * 		Exact destination register.
	 * @param from
	 * 		Exact source register.
	 *
	 * @return Matcher against a move with the given registers, regardless of encoding width.
	 */
	@Nonnull
	public static DexMoveInstructionMatcher exact(int to, int from) {
		return new DexMoveInstructionMatcher(Matchers.any(), Matchers.exact(to), Matchers.exact(from));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof MoveInstruction(int iopcode, int ito, int ifrom)
				&& opcode.matches(iopcode)
				&& to.matches(ito)
				&& from.matches(ifrom);
	}
}
