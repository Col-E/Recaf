package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code new-array} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param sizeRegister
 * 		Array-size register matcher.
 * @param componentType
 * 		Array component class descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexNewArrayInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                            @Nonnull Matcher<Integer> dest,
                                            @Nonnull Matcher<Integer> sizeRegister,
                                            @Nonnull Matcher<String> componentType) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param sizeRegister
	 * 		Exact array-size register.
	 * @param componentType
	 * 		Exact component class descriptor.
	 *
	 * @return Matcher against the exact new-array instruction.
	 */
	@Nonnull
	public static DexNewArrayInstructionMatcher exact(int opcode, int dest, int sizeRegister,
	                                                  @Nonnull String componentType) {
		return new DexNewArrayInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest),
				Matchers.exact(sizeRegister), Matchers.exact(componentType));
	}

	/**
	 * @param dest
	 * 		Exact destination register.
	 * @param sizeRegister
	 * 		Exact array-size register.
	 * @param componentType
	 * 		Exact component class descriptor.
	 *
	 * @return Matcher against a new-array with the given operands.
	 */
	@Nonnull
	public static DexNewArrayInstructionMatcher exact(int dest, int sizeRegister,
	                                                  @Nonnull String componentType) {
		return new DexNewArrayInstructionMatcher(Matchers.any(), Matchers.exact(dest),
				Matchers.exact(sizeRegister), Matchers.exact(componentType));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof NewArrayInstruction array
				&& opcode.matches(array.opcode())
				&& dest.matches(array.dest())
				&& sizeRegister.matches(array.sizeRegister())
				&& componentType.matches(array.componentType().descriptor());
	}
}
