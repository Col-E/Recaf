package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a two-register DEX conditional branch.
 *
 * @param opcode
 * 		Branch opcode matcher.
 * @param a
 * 		First register matcher.
 * @param b
 * 		Second register matcher.
 * @param label
 * 		Branch target matcher.
 *
 * @author Matt Coley
 */
public record DexBranchInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                          @Nonnull Matcher<Integer> a,
                                          @Nonnull Matcher<Integer> b,
                                          @Nonnull Matcher<Label> label) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 *
	 * @return Matcher against any two-register branch with the given opcode.
	 */
	@Nonnull
	public static DexBranchInstructionMatcher op(int opcode) {
		return new DexBranchInstructionMatcher(Matchers.exact(opcode), Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @return Matcher that matches any two-register conditional branch.
	 */
	@Nonnull
	public static DexBranchInstructionMatcher any() {
		return new DexBranchInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 * @param a
	 * 		First register to match.
	 * @param b
	 * 		Second register to match.
	 * @param label
	 * 		Branch target to match.
	 *
	 * @return Matcher against the exact branch operands.
	 */
	@Nonnull
	public static DexBranchInstructionMatcher exact(int opcode, int a, int b, @Nonnull Label label) {
		return exact(opcode, a, b, Matchers.exact(label));
	}

	/**
	 * @param opcode
	 * 		Branch opcode to match.
	 * @param a
	 * 		First register to match.
	 * @param b
	 * 		Second register to match.
	 * @param label
	 * 		Branch target matcher.
	 *
	 * @return Matcher against the exact branch registers and the supplied target constraint.
	 */
	@Nonnull
	public static DexBranchInstructionMatcher exact(int opcode, int a, int b, @Nonnull Matcher<Label> label) {
		return new DexBranchInstructionMatcher(Matchers.exact(opcode), Matchers.exact(a), Matchers.exact(b), label);
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof BranchInstruction branch
				&& opcode.matches(branch.opcode())
				&& a.matches(branch.a())
				&& b.matches(branch.b())
				&& label.matches(branch.label());
	}
}
