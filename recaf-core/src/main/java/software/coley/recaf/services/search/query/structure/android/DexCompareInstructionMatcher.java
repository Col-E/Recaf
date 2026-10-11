package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.CompareInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX floating-point or wide comparison instruction.
 *
 * @param opcode
 * 		Comparison opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param a
 * 		First source register matcher.
 * @param b
 * 		Second source register matcher.
 *
 * @author Matt Coley
 */
public record DexCompareInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                           @Nonnull Matcher<Integer> dest,
                                           @Nonnull Matcher<Integer> a,
                                           @Nonnull Matcher<Integer> b) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact comparison opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param a
	 * 		Exact first source register.
	 * @param b
	 * 		Exact second source register.
	 *
	 * @return Matcher against the exact comparison instruction.
	 */
	@Nonnull
	public static DexCompareInstructionMatcher exact(int opcode, int dest, int a, int b) {
		return new DexCompareInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest),
				Matchers.exact(a), Matchers.exact(b));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof CompareInstruction(int iopcode, int idest, int ia, int ib)
				&& opcode.matches(iopcode)
				&& dest.matches(idest)
				&& a.matches(ia)
				&& b.matches(ib);
	}
}
