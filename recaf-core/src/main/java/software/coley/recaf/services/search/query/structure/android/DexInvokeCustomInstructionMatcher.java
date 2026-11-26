package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.Handle;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a DEX invoke-custom instruction.
 *
 * <p>The call-site constants in {@link InvokeCustomInstruction#arguments()} are separate from the registers
 * consumed by the invocation. The former are matched by {@link #arguments()}, while the latter are matched by
 * {@link #argumentRegisters()}.</p>
 *
 * @param opcode
 * 		Canonical invoke-custom opcode matcher. The DEX model reports the non-range opcode for both encodings.
 * @param handle
 * 		Bootstrap method handle matcher.
 * @param name
 * 		Call-site name matcher.
 * @param descriptor
 * 		Call-site method descriptor matcher.
 * @param arguments
 * 		Call-site/bootstrap constant matcher.
 * @param argumentRegisters
 * 		Invocation register matcher. Range forms are expanded to their implied consecutive register list.
 * @param isRange
 * 		Encoding-form matcher: {@code true} for {@code invoke-custom/range}.
 *
 * @author Matt Coley
 */
public record DexInvokeCustomInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                @Nonnull Matcher<Handle> handle,
                                                @Nonnull Matcher<String> name,
                                                @Nonnull Matcher<String> descriptor,
                                                @Nonnull ListMatcher<Matcher<? extends Object>> arguments,
                                                @Nonnull ListMatcher<Matcher<Integer>> argumentRegisters,
                                                @Nonnull Matcher<Boolean> isRange) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Canonical invoke-custom opcode to match.
	 *
	 * @return Matcher against any invoke-custom instruction with the given canonical opcode.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher op(int opcode) {
		return new DexInvokeCustomInstructionMatcher(
				Matchers.exact(opcode), Matchers.any(), Matchers.any(), Matchers.any(),
				ListMatcher.any(), ListMatcher.any(), Matchers.any()
		);
	}

	/**
	 * @return Matcher that accepts any invoke-custom encoding and operands.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher any() {
		return op(Opcodes.INVOKE_CUSTOM);
	}

	/**
	 * @return Matcher that accepts any invoke-custom range encoding and operands.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher range() {
		return new DexInvokeCustomInstructionMatcher(
				Matchers.exact(Opcodes.INVOKE_CUSTOM), Matchers.any(), Matchers.any(), Matchers.any(),
				ListMatcher.any(), ListMatcher.any(), Matchers.exact(true)
		);
	}

	/**
	 * @param size
	 * 		Number of registers in the range.
	 * @param first
	 * 		First register in the range.
	 *
	 * @return Matcher for any invoke-custom range with the given register run.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher range(int size, int first) {
		return new DexInvokeCustomInstructionMatcher(
				Matchers.exact(Opcodes.INVOKE_CUSTOM), Matchers.any(), Matchers.any(), Matchers.any(),
				ListMatcher.any(), exactRegisters(rangeRegisters(size, first)), Matchers.exact(true)
		);
	}

	/**
	 * @param handle
	 * 		Bootstrap method handle to match.
	 * @param name
	 * 		Call-site name to match.
	 * @param descriptor
	 * 		Call-site descriptor to match.
	 * @param arguments
	 * 		Call-site/bootstrap constants to match.
	 * @param argumentRegisters
	 * 		Explicit invocation registers to match.
	 *
	 * @return Matcher against an exact non-range invoke-custom call site.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher exact(@Nonnull Handle handle,
	                                                      @Nonnull String name,
	                                                      @Nonnull String descriptor,
	                                                      @Nonnull List<Constant> arguments,
	                                                      int... argumentRegisters) {
		return new DexInvokeCustomInstructionMatcher(
				Matchers.exact(Opcodes.INVOKE_CUSTOM),
				Matchers.exact(handle),
				Matchers.exact(name),
				Matchers.exact(descriptor),
				exactArguments(arguments),
				exactRegisters(argumentRegisters),
				Matchers.exact(false)
		);
	}

	/**
	 * @param handle
	 * 		Bootstrap method handle to match.
	 * @param name
	 * 		Call-site name to match.
	 * @param descriptor
	 * 		Call-site descriptor to match.
	 * @param arguments
	 * 		Call-site/bootstrap constants to match.
	 * @param size
	 * 		Number of registers in the range.
	 * @param first
	 * 		First register in the range.
	 *
	 * @return Matcher against an exact range-form invoke-custom call site.
	 */
	@Nonnull
	public static DexInvokeCustomInstructionMatcher exactRange(@Nonnull Handle handle,
	                                                           @Nonnull String name,
	                                                           @Nonnull String descriptor,
	                                                           @Nonnull List<Constant> arguments,
	                                                           int size,
	                                                           int first) {
		return new DexInvokeCustomInstructionMatcher(
				Matchers.exact(Opcodes.INVOKE_CUSTOM),
				Matchers.exact(handle),
				Matchers.exact(name),
				Matchers.exact(descriptor),
				exactArguments(arguments),
				exactRegisters(rangeRegisters(size, first)),
				Matchers.exact(true)
		);
	}

	/**
	 * @param arguments
	 * 		Call-site/bootstrap constants to match.
	 *
	 * @return Exact sequence matcher for the supplied constants.
	 */
	@Nonnull
	private static ListMatcher<Matcher<? extends Object>> exactArguments(@Nonnull List<Constant> arguments) {
		List<Matcher<? extends Object>> matchers = new ArrayList<>(arguments.size());
		for (Constant argument : arguments)
			matchers.add(Matchers.exact(argument));
		return ListMatcher.sequence(matchers);
	}

	/**
	 * @param argumentRegisters
	 * 		Registers to match.
	 *
	 * @return Exact sequence matcher for the supplied registers.
	 */
	@Nonnull
	private static ListMatcher<Matcher<Integer>> exactRegisters(int[] argumentRegisters) {
		List<Matcher<Integer>> matchers = new ArrayList<>(argumentRegisters.length);
		for (int argumentRegister : argumentRegisters)
			matchers.add(Matchers.exact(argumentRegister));
		return ListMatcher.sequence(matchers);
	}

	/**
	 * Expands a range-form register run without reading the nullable explicit register array.
	 */
	@Nonnull
	private static int[] rangeRegisters(int size, int first) {
		if (size < 0)
			throw new IllegalArgumentException("Register range size must not be negative");
		int[] registers = new int[size];
		for (int offset = 0; offset < size; offset++)
			registers[offset] = first + offset;
		return registers;
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		if (!(instruction instanceof InvokeCustomInstruction invoke)
				|| !opcode.matches(invoke.opcode())
				|| !handle.matches(invoke.handle())
				|| !name.matches(invoke.name())
				|| !descriptor.matches(invoke.type().descriptor())
				|| !arguments.matches(invoke.arguments())
				|| !isRange.matches(invoke.isRange()))
			return false;

		return argumentRegisters.matches(DexRegisterList.of(invoke));
	}

}
