package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches an unconditional DEX branch.
 *
 * @param opcode
 * 		Goto opcode matcher.
 * @param jump
 * 		Branch target matcher.
 *
 * @author Matt Coley
 */
public record DexGotoInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                        @Nonnull Matcher<Label> jump) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Goto opcode to match.
	 *
	 * @return Matcher against any goto with the given opcode.
	 */
	@Nonnull
	public static DexGotoInstructionMatcher op(int opcode) {
		return new DexGotoInstructionMatcher(Matchers.exact(opcode), Matchers.any());
	}

	/**
	 * @return Matcher that matches any DEX goto instruction.
	 */
	@Nonnull
	public static DexGotoInstructionMatcher any() {
		return new DexGotoInstructionMatcher(Matchers.any(), Matchers.any());
	}

	/**
	 * @param opcode
	 * 		Goto opcode to match.
	 * @param jump
	 * 		Branch target to match.
	 *
	 * @return Matcher against the exact goto operands.
	 */
	@Nonnull
	public static DexGotoInstructionMatcher exact(int opcode, @Nonnull Label jump) {
		return exact(opcode, Matchers.exact(jump));
	}

	/**
	 * @param opcode
	 * 		Goto opcode to match.
	 * @param jump
	 * 		Branch target matcher.
	 *
	 * @return Matcher against the exact opcode and supplied target constraint.
	 */
	@Nonnull
	public static DexGotoInstructionMatcher exact(int opcode, @Nonnull Matcher<Label> jump) {
		return new DexGotoInstructionMatcher(Matchers.exact(opcode), jump);
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof GotoInstruction(int iopcode, Label ijump)
				&& opcode.matches(iopcode)
				&& jump.matches(ijump);
	}
}
