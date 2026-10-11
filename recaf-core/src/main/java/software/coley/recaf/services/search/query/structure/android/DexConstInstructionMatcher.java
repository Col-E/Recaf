package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX integer {@code const} instruction.
 *
 * @param opcode
 * 		Opcode matcher. This can distinguish the {@code 4}, {@code 16}, {@code high16}, and full-width forms.
 * @param register
 * 		Destination register matcher.
 * @param value
 * 		Signed integer payload matcher.
 *
 * @author Matt Coley
 */
public record DexConstInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                         @Nonnull Matcher<Integer> register,
                                         @Nonnull Matcher<Integer> value) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact constant opcode.
	 * @param register
	 * 		Exact destination register.
	 * @param value
	 * 		Exact signed integer payload.
	 *
	 * @return Matcher against the exact integer constant instruction.
	 */
	@Nonnull
	public static DexConstInstructionMatcher exact(int opcode, int register, int value) {
		return new DexConstInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(value));
	}

	/**
	 * @param register
	 * 		Exact destination register.
	 * @param value
	 * 		Exact signed integer payload.
	 *
	 * @return Matcher against an integer constant with the given operands, regardless of encoding width.
	 */
	@Nonnull
	public static DexConstInstructionMatcher exact(int register, int value) {
		return new DexConstInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(value));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ConstInstruction(int iopcode, int iregister, int ivalue)
				&& opcode.matches(iopcode)
				&& register.matches(iregister)
				&& value.matches(ivalue);
	}
}
