package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.FillArrayDataInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

import java.util.Arrays;

/**
 * Matches a DEX {@code fill-array-data} payload instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param array
 * 		Array reference register matcher.
 * @param data
 * 		Payload-byte matcher.
 * @param elementSize
 * 		Element-width matcher in bytes.
 *
 * @author Matt Coley
 */
public record DexFillArrayDataInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                 @Nonnull Matcher<Integer> array,
                                                 @Nonnull Matcher<byte[]> data,
                                                 @Nonnull Matcher<Integer> elementSize) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param array
	 * 		Exact array reference register.
	 * @param data
	 * 		Matcher for the decoded payload bytes.
	 * @param elementSize
	 * 		Exact element width in bytes.
	 *
	 * @return Matcher against the exact fill-array-data instruction.
	 */
	@Nonnull
	public static DexFillArrayDataInstructionMatcher exact(int opcode, int array, @Nonnull byte[] data, int elementSize) {
		return new DexFillArrayDataInstructionMatcher(Matchers.exact(opcode), Matchers.exact(array),
				Matchers.predicate(actual -> actual != null && Arrays.equals(actual, data)), Matchers.exact(elementSize));
	}

	/**
	 * @param array
	 * 		Exact array reference register.
	 * @param data
	 * 		Matcher for the decoded payload bytes.
	 * @param elementSize
	 * 		Exact element width in bytes.
	 *
	 * @return Matcher against a fill-array-data instruction with the given payload.
	 */
	@Nonnull
	public static DexFillArrayDataInstructionMatcher exact(int array, @Nonnull byte[] data, int elementSize) {
		return new DexFillArrayDataInstructionMatcher(Matchers.any(), Matchers.exact(array),
				Matchers.predicate(actual -> actual != null && Arrays.equals(actual, data)), Matchers.exact(elementSize));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof FillArrayDataInstruction fillArray
				&& opcode.matches(fillArray.opcode())
				&& array.matches(fillArray.array())
				&& data.matches(fillArray.data())
				&& elementSize.matches(fillArray.elementSize());
	}
}
