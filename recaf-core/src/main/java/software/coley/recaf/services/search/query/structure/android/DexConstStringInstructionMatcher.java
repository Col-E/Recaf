package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstStringInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code const-string} instruction.
 *
 * @param opcode
 * 		Opcode matcher. This can distinguish {@code const-string} from {@code const-string/jumbo}.
 * @param register
 * 		Destination register matcher.
 * @param string
 * 		String-pool payload matcher.
 *
 * @author Matt Coley
 */
public record DexConstStringInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                               @Nonnull Matcher<Integer> register,
                                               @Nonnull Matcher<String> string) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact string constant opcode.
	 * @param register
	 * 		Exact destination register.
	 * @param string
	 * 		Exact string payload.
	 *
	 * @return Matcher against the exact string constant instruction.
	 */
	@Nonnull
	public static DexConstStringInstructionMatcher exact(int opcode, int register, @Nonnull String string) {
		return new DexConstStringInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(string));
	}

	/**
	 * @param register
	 * 		Exact destination register.
	 * @param string
	 * 		Exact string payload.
	 *
	 * @return Matcher against a string constant with the given operands, regardless of pool-index width.
	 */
	@Nonnull
	public static DexConstStringInstructionMatcher exact(int register, @Nonnull String string) {
		return new DexConstStringInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(string));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ConstStringInstruction(int iopcode, int iregister, String istring)
				&& opcode.matches(iopcode)
				&& register.matches(iregister)
				&& string.matches(istring);
	}
}
