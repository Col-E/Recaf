package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code check-cast} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param register
 * 		Register matcher.
 * @param type
 * 		Class descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexCheckCastInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                             @Nonnull Matcher<Integer> register,
                                             @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param register
	 * 		Exact register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against the exact check-cast instruction.
	 */
	@Nonnull
	public static DexCheckCastInstructionMatcher exact(int opcode, int register, @Nonnull String type) {
		return new DexCheckCastInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(type));
	}

	/**
	 * @param register
	 * 		Exact register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against a check-cast with the given operands.
	 */
	@Nonnull
	public static DexCheckCastInstructionMatcher exact(int register, @Nonnull String type) {
		return new DexCheckCastInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof CheckCastInstruction cast
				&& opcode.matches(cast.opcode())
				&& register.matches(cast.register())
				&& type.matches(cast.type().descriptor());
	}
}
