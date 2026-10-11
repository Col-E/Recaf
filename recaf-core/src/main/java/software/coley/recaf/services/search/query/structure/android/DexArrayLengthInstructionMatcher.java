package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ArrayLengthInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code array-length} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param array
 * 		Array reference register matcher.
 *
 * @author Matt Coley
 */
public record DexArrayLengthInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                               @Nonnull Matcher<Integer> dest,
                                               @Nonnull Matcher<Integer> array) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param array
	 * 		Exact array reference register.
	 *
	 * @return Matcher against the exact array-length instruction.
	 */
	@Nonnull
	public static DexArrayLengthInstructionMatcher exact(int opcode, int dest, int array) {
		return new DexArrayLengthInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest), Matchers.exact(array));
	}

	/**
	 * @param dest
	 * 		Exact destination register.
	 * @param array
	 * 		Exact array reference register.
	 *
	 * @return Matcher against an array-length instruction with the given registers.
	 */
	@Nonnull
	public static DexArrayLengthInstructionMatcher exact(int dest, int array) {
		return new DexArrayLengthInstructionMatcher(Matchers.any(), Matchers.exact(dest), Matchers.exact(array));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ArrayLengthInstruction value
				&& opcode.matches(value.opcode())
				&& dest.matches(value.dest())
				&& array.matches(value.array());
	}
}
