package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code move-result} instruction.
 *
 * @param opcode
 * 		Opcode matcher. This can distinguish normal, wide, and object results.
 * @param type
 * 		Result-kind matcher, using the model's {@code Result} constants.
 * @param to
 * 		Destination register matcher.
 *
 * @author Matt Coley
 */
public record DexMoveResultInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                              @Nonnull Matcher<Integer> type,
                                              @Nonnull Matcher<Integer> to) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact result opcode.
	 * @param type
	 * 		Exact result kind.
	 * @param to
	 * 		Exact destination register.
	 *
	 * @return Matcher against the exact move-result instruction.
	 */
	@Nonnull
	public static DexMoveResultInstructionMatcher exact(int opcode, int type, int to) {
		return new DexMoveResultInstructionMatcher(Matchers.exact(opcode), Matchers.exact(type), Matchers.exact(to));
	}

	/**
	 * @param type
	 * 		Exact result kind.
	 * @param to
	 * 		Exact destination register.
	 *
	 * @return Matcher against the given result kind and register.
	 */
	@Nonnull
	public static DexMoveResultInstructionMatcher exact(int type, int to) {
		return new DexMoveResultInstructionMatcher(Matchers.any(), Matchers.exact(type), Matchers.exact(to));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof MoveResultInstruction(int iopcode, int itype, int ito)
				&& opcode.matches(iopcode)
				&& type.matches(itype)
				&& to.matches(ito);
	}
}
