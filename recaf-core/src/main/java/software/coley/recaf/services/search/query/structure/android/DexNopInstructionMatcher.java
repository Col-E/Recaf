package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.NopInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code nop} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 *
 * @author Matt Coley
 */
public record DexNopInstructionMatcher(@Nonnull Matcher<Integer> opcode) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 *
	 * @return Matcher against the exact nop opcode.
	 */
	@Nonnull
	public static DexNopInstructionMatcher exact(int opcode) {
		return new DexNopInstructionMatcher(Matchers.exact(opcode));
	}

	/**
	 * @return Matcher against a DEX nop instruction.
	 */
	@Nonnull
	public static DexNopInstructionMatcher any() {
		return new DexNopInstructionMatcher(Matchers.any());
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof NopInstruction value && opcode.matches(value.opcode());
	}
}
