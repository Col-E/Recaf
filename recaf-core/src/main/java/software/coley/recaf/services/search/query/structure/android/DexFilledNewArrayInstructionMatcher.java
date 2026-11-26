package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a DEX {@code filled-new-array} instruction, including range and explicit register forms.
 *
 * @param opcode
 * 		Opcode matcher. This distinguishes the explicit and {@code /range} encodings.
 * @param componentType
 * 		Array component class descriptor matcher.
 * @param first
 * 		First register matcher. For the explicit form this is the first listed register; for the range form it
 * 		is the first register in the range.
 * @param size
 * 		Number-of-registers matcher.
 * @param registers
 * 		Optional ordered matcher for the explicit register list. A {@code null} matcher leaves the list
 * 		unconstrained and permits the range form to be matched using {@code first} and {@code size}.
 *
 * @author Matt Coley
 */
public record DexFilledNewArrayInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                  @Nonnull Matcher<String> componentType,
                                                  @Nonnull Matcher<Integer> first,
                                                  @Nonnull Matcher<Integer> size,
                                                  @Nullable ListMatcher<Matcher<Integer>> registers)
		implements DexInsnMatcher {
	/**
	 * Creates a matcher without an explicit register-list constraint.
	 */
	public DexFilledNewArrayInstructionMatcher(@Nonnull Matcher<Integer> opcode,
	                                           @Nonnull Matcher<String> componentType,
	                                           @Nonnull Matcher<Integer> first,
	                                           @Nonnull Matcher<Integer> size) {
		this(opcode, componentType, first, size, null);
	}

	/**
	 * @param opcode
	 * 		Exact filled-array opcode.
	 * @param componentType
	 * 		Exact component class descriptor.
	 * @param registers
	 * 		Exact explicit register list. The list must be non-empty, as required by the model.
	 *
	 * @return Matcher against the exact explicit filled-new-array instruction.
	 */
	@Nonnull
	public static DexFilledNewArrayInstructionMatcher exact(int opcode, @Nonnull String componentType,
	                                                        int... registers) {
		if (registers.length == 0)
			throw new IllegalArgumentException("Filled-new-array register list must not be empty");
		List<Matcher<Integer>> matchers = new ArrayList<>(registers.length);
		for (int register : registers)
			matchers.add(Matchers.exact(register));
		return new DexFilledNewArrayInstructionMatcher(Matchers.exact(opcode), Matchers.exact(componentType),
				Matchers.exact(registers[0]), Matchers.exact(registers.length), ListMatcher.sequence(matchers));
	}

	/**
	 * @param opcode
	 * 		Exact range opcode.
	 * @param componentType
	 * 		Exact component class descriptor.
	 * @param size
	 * 		Exact number of registers in the range.
	 * @param first
	 * 		Exact first register in the range.
	 *
	 * @return Matcher against the exact range-form filled-new-array instruction.
	 */
	@Nonnull
	public static DexFilledNewArrayInstructionMatcher exactRange(int opcode, @Nonnull String componentType,
	                                                             int size, int first) {
		return new DexFilledNewArrayInstructionMatcher(Matchers.exact(opcode), Matchers.exact(componentType),
				Matchers.exact(first), Matchers.exact(size));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		if (!(instruction instanceof FilledNewArrayInstruction fillNewArray)
				|| !opcode.matches(fillNewArray.opcode())
				|| !componentType.matches(fillNewArray.componentType().descriptor())
				|| !first.matches(fillNewArray.first()))
			return false;

		int[] actualRegisters = fillNewArray.registers();
		int actualSize = actualRegisters == null ? fillNewArray.last() - fillNewArray.first() + 1 : actualRegisters.length;
		if (!size.matches(actualSize))
			return false;

		if (registers == null)
			return true;
		if (actualRegisters == null)
			return false;
		return registers.matches(DexRegisterList.of(actualRegisters));
	}
}
