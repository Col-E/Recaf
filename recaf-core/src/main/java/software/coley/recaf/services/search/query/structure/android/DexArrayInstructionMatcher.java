package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX array get/put instruction and all register operands.
 *
 * @param opcode
 * 		Opcode matcher. This distinguishes get/put and the array element kind.
 * @param kind
 * 		Array element-kind offset matcher as represented by the DEX model.
 * @param value
 * 		Value register matcher.
 * @param array
 * 		Array reference register matcher.
 * @param index
 * 		Array index register matcher.
 *
 * @author Matt Coley
 */
public record DexArrayInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                         @Nonnull Matcher<Integer> kind,
                                         @Nonnull Matcher<Integer> value,
                                         @Nonnull Matcher<Integer> array,
                                         @Nonnull Matcher<Integer> index) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact array opcode.
	 * @param value
	 * 		Exact value register.
	 * @param array
	 * 		Exact array reference register.
	 * @param index
	 * 		Exact index register.
	 *
	 * @return Matcher against the exact array instruction.
	 */
	@Nonnull
	public static DexArrayInstructionMatcher exact(int opcode, int value, int array, int index) {
		return new DexArrayInstructionMatcher(Matchers.exact(opcode), Matchers.any(), Matchers.exact(value),
				Matchers.exact(array), Matchers.exact(index));
	}

	/**
	 * @param value
	 * 		Exact value register.
	 * @param array
	 * 		Exact array reference register.
	 * @param index
	 * 		Exact index register.
	 *
	 * @return Matcher against an array instruction with the given registers.
	 */
	@Nonnull
	public static DexArrayInstructionMatcher exact(int value, int array, int index) {
		return new DexArrayInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.exact(value),
				Matchers.exact(array), Matchers.exact(index));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ArrayInstruction arrayInsn
				&& opcode.matches(arrayInsn.opcode())
				&& kind.matches(arrayInsn.kind())
				&& value.matches(arrayInsn.value())
				&& array.matches(arrayInsn.array())
				&& index.matches(arrayInsn.index());
	}
}
