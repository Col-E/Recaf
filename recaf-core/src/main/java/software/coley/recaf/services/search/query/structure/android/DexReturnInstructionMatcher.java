package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX return instruction, including {@code return-void}.
 *
 * @param opcode
 * 		Opcode matcher. This distinguishes normal, wide, object, and void returns.
 * @param register
 * 		Returned-value register matcher. The model uses {@code -1} for void returns.
 * @param type
 * 		Return-kind matcher, using the model's {@code Return} constants.
 *
 * @author Matt Coley
 */
public record DexReturnInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                          @Nonnull Matcher<Integer> register,
                                          @Nonnull Matcher<Integer> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact return opcode.
	 * @param register
	 * 		Exact returned-value register, or {@code -1} for void.
	 * @param type
	 * 		Exact return kind.
	 *
	 * @return Matcher against the exact return instruction.
	 */
	@Nonnull
	public static DexReturnInstructionMatcher exact(int opcode, int register, int type) {
		return new DexReturnInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(type));
	}

	/**
	 * @param register
	 * 		Exact returned-value register, or {@code -1} for void.
	 * @param type
	 * 		Exact return kind.
	 *
	 * @return Matcher against a return with the given operands, regardless of opcode spelling.
	 */
	@Nonnull
	public static DexReturnInstructionMatcher exact(int register, int type) {
		return new DexReturnInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ReturnInstruction(int iopcode, int iregister, int itype)
				&& opcode.matches(iopcode)
				&& register.matches(iregister)
				&& type.matches(itype);
	}
}
