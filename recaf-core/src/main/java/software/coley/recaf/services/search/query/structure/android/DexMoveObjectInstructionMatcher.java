package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.MoveObjectInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code move-object} instruction and its register operands.
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
public record DexMoveObjectInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                              @Nonnull Matcher<Integer> to,
                                              @Nonnull Matcher<Integer> from) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact move-object opcode.
	 * @param to
	 * 		Exact destination register.
	 * @param from
	 * 		Exact source register.
	 *
	 * @return Matcher against the exact move-object instruction.
	 */
	@Nonnull
	public static DexMoveObjectInstructionMatcher exact(int opcode, int to, int from) {
		return new DexMoveObjectInstructionMatcher(Matchers.exact(opcode), Matchers.exact(to), Matchers.exact(from));
	}

	/**
	 * @param to
	 * 		Exact destination register.
	 * @param from
	 * 		Exact source register.
	 *
	 * @return Matcher against a move-object with the given registers, regardless of encoding width.
	 */
	@Nonnull
	public static DexMoveObjectInstructionMatcher exact(int to, int from) {
		return new DexMoveObjectInstructionMatcher(Matchers.any(), Matchers.exact(to), Matchers.exact(from));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof MoveObjectInstruction(int iopcode, int ito, int ifrom)
				&& opcode.matches(iopcode)
				&& to.matches(ito)
				&& from.matches(ifrom);
	}
}
