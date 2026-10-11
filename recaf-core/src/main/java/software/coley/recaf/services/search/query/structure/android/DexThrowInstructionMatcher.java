package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code throw} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param value
 * 		Exception-object register matcher.
 *
 * @author Matt Coley
 */
public record DexThrowInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                         @Nonnull Matcher<Integer> value) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param value
	 * 		Exact exception-object register.
	 *
	 * @return Matcher against the exact throw instruction.
	 */
	@Nonnull
	public static DexThrowInstructionMatcher exact(int opcode, int value) {
		return new DexThrowInstructionMatcher(Matchers.exact(opcode), Matchers.exact(value));
	}

	/**
	 * @param value
	 * 		Exact exception-object register.
	 *
	 * @return Matcher against a throw from the given register.
	 */
	@Nonnull
	public static DexThrowInstructionMatcher exact(int value) {
		return new DexThrowInstructionMatcher(Matchers.any(), Matchers.exact(value));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ThrowInstruction throwInsn
				&& opcode.matches(throwInsn.opcode())
				&& value.matches(throwInsn.value());
	}
}
