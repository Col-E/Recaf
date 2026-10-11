package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.MoveExceptionInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code move-exception} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param register
 * 		Destination register matcher.
 *
 * @author Matt Coley
 */
public record DexMoveExceptionInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                 @Nonnull Matcher<Integer> register) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param register
	 * 		Exact destination register.
	 *
	 * @return Matcher against the exact move-exception instruction.
	 */
	@Nonnull
	public static DexMoveExceptionInstructionMatcher exact(int opcode, int register) {
		return new DexMoveExceptionInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register));
	}

	/**
	 * @param register
	 * 		Exact destination register.
	 *
	 * @return Matcher against a move-exception writing the given register.
	 */
	@Nonnull
	public static DexMoveExceptionInstructionMatcher exact(int register) {
		return new DexMoveExceptionInstructionMatcher(Matchers.any(), Matchers.exact(register));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof MoveExceptionInstruction move
				&& opcode.matches(move.opcode())
				&& register.matches(move.register());
	}
}
