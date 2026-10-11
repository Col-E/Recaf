package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.UnaryInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX unary numeric conversion or negation instruction.
 *
 * @param opcode
 * 		Unary opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param source
 * 		Source register matcher.
 *
 * @author Matt Coley
 */
public record DexUnaryInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                         @Nonnull Matcher<Integer> dest,
                                         @Nonnull Matcher<Integer> source) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact unary opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param source
	 * 		Exact source register.
	 *
	 * @return Matcher against the exact unary instruction.
	 */
	@Nonnull
	public static DexUnaryInstructionMatcher exact(int opcode, int dest, int source) {
		return new DexUnaryInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest), Matchers.exact(source));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof UnaryInstruction(int iopcode, int idest, int isource)
				&& opcode.matches(iopcode)
				&& dest.matches(idest)
				&& source.matches(isource);
	}
}
