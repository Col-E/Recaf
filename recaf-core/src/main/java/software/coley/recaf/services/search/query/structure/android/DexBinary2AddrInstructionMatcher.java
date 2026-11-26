package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX two-address binary arithmetic or bitwise instruction.
 *
 * @param opcode
 * 		Binary opcode matcher.
 * @param a
 * 		Destination/first source register matcher.
 * @param b
 * 		Second source register matcher.
 *
 * @author Matt Coley
 */
public record DexBinary2AddrInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                               @Nonnull Matcher<Integer> a,
                                               @Nonnull Matcher<Integer> b) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact binary opcode.
	 * @param a
	 * 		Exact destination/first source register.
	 * @param b
	 * 		Exact second source register.
	 *
	 * @return Matcher against the exact two-address binary instruction.
	 */
	@Nonnull
	public static DexBinary2AddrInstructionMatcher exact(int opcode, int a, int b) {
		return new DexBinary2AddrInstructionMatcher(Matchers.exact(opcode), Matchers.exact(a), Matchers.exact(b));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof Binary2AddrInstruction(int iopcode, int ia, int ib)
				&& opcode.matches(iopcode)
				&& a.matches(ia)
				&& b.matches(ib);
	}
}
