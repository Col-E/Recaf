package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.BranchZeroInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a one-register DEX conditional branch whose other operand is zero.
 *
 * @param opcode
 * 		Branch opcode matcher.
 * @param a
 * 		Register matcher.
 * @param label
 * 		Branch target matcher.
 *
 * @author Matt Coley
 */
public record DexBranchZeroInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                              @Nonnull Matcher<Integer> a,
                                              @Nonnull Matcher<Label> label) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 *
	 * @return Matcher against any zero-branch with the given opcode.
	 */
	@Nonnull
	public static DexBranchZeroInstructionMatcher op(int opcode) {
		return new DexBranchZeroInstructionMatcher(Matchers.exact(opcode), Matchers.any(), Matchers.any());
	}

	/**
	 * @return Matcher that matches any one-register zero-branch.
	 */
	@Nonnull
	public static DexBranchZeroInstructionMatcher any() {
		return new DexBranchZeroInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 * @param a
	 * 		Register to match.
	 * @param label
	 * 		Branch target to match.
	 *
	 * @return Matcher against the exact branch operands.
	 */
	@Nonnull
	public static DexBranchZeroInstructionMatcher exact(int opcode, int a, @Nonnull Label label) {
		return exact(opcode, a, Matchers.exact(label));
	}

	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 * @param a
	 * 		Register to match.
	 * @param label
	 * 		Branch target matcher.
	 *
	 * @return Matcher against the exact register and supplied target constraint.
	 */
	@Nonnull
	public static DexBranchZeroInstructionMatcher exact(int opcode, int a, @Nonnull Matcher<Label> label) {
		return new DexBranchZeroInstructionMatcher(Matchers.exact(opcode), Matchers.exact(a), label);
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof BranchZeroInstruction branch
				&& opcode.matches(branch.opcode())
				&& a.matches(branch.a())
				&& label.matches(branch.label());
	}
}
