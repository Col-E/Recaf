package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code new-instance} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param dest
 * 		Destination register matcher.
 * @param type
 * 		Instance class descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexNewInstanceInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                               @Nonnull Matcher<Integer> dest,
                                               @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param dest
	 * 		Exact destination register.
	 * @param type
	 * 		Exact instance class descriptor.
	 *
	 * @return Matcher against the exact new-instance instruction.
	 */
	@Nonnull
	public static DexNewInstanceInstructionMatcher exact(int opcode, int dest, @Nonnull String type) {
		return new DexNewInstanceInstructionMatcher(Matchers.exact(opcode), Matchers.exact(dest), Matchers.exact(type));
	}

	/**
	 * @param dest
	 * 		Exact destination register.
	 * @param type
	 * 		Exact instance class descriptor.
	 *
	 * @return Matcher against a new-instance with the given operands.
	 */
	@Nonnull
	public static DexNewInstanceInstructionMatcher exact(int dest, @Nonnull String type) {
		return new DexNewInstanceInstructionMatcher(Matchers.any(), Matchers.exact(dest), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof NewInstanceInstruction instance
				&& opcode.matches(instance.opcode())
				&& dest.matches(instance.dest())
				&& type.matches(instance.type().descriptor());
	}
}
