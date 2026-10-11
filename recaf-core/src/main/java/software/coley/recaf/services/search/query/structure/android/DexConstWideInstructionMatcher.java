package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX wide integer {@code const-wide} instruction.
 *
 * @param opcode
 * 		Opcode matcher. This can distinguish the {@code 16}, {@code 32}, {@code high16}, and full-width forms.
 * @param register
 * 		Destination register matcher.
 * @param value
 * 		Signed long payload matcher.
 *
 * @author Matt Coley
 */
public record DexConstWideInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                             @Nonnull Matcher<Integer> register,
                                             @Nonnull Matcher<Long> value) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact constant opcode.
	 * @param register
	 * 		Exact destination register.
	 * @param value
	 * 		Exact signed long payload.
	 *
	 * @return Matcher against the exact wide constant instruction.
	 */
	@Nonnull
	public static DexConstWideInstructionMatcher exact(int opcode, int register, long value) {
		return new DexConstWideInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(value));
	}

	/**
	 * @param register
	 * 		Exact destination register.
	 * @param value
	 * 		Exact signed long payload.
	 *
	 * @return Matcher against a wide constant with the given operands, regardless of encoding width.
	 */
	@Nonnull
	public static DexConstWideInstructionMatcher exact(int register, long value) {
		return new DexConstWideInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(value));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ConstWideInstruction(int iopcode, int iregister, long ivalue)
				&& opcode.matches(iopcode)
				&& register.matches(iregister)
				&& value.matches(ivalue);
	}
}
