package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code const-method-type} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param destination
 * 		Destination register matcher.
 * @param type
 * 		Method descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexConstMethodTypeInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                   @Nonnull Matcher<Integer> destination,
                                                   @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param destination
	 * 		Exact destination register.
	 * @param type
	 * 		Exact method descriptor.
	 *
	 * @return Matcher against the exact method-type constant instruction.
	 */
	@Nonnull
	public static DexConstMethodTypeInstructionMatcher exact(int opcode, int destination, @Nonnull String type) {
		return new DexConstMethodTypeInstructionMatcher(Matchers.exact(opcode), Matchers.exact(destination), Matchers.exact(type));
	}

	/**
	 * @param destination
	 * 		Exact destination register.
	 * @param type
	 * 		Exact method descriptor.
	 *
	 * @return Matcher against a method-type constant with the given operands.
	 */
	@Nonnull
	public static DexConstMethodTypeInstructionMatcher exact(int destination, @Nonnull String type) {
		return new DexConstMethodTypeInstructionMatcher(Matchers.any(), Matchers.exact(destination), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ConstMethodTypeInstruction method
				&& opcode.matches(method.opcode())
				&& destination.matches(method.destination())
				&& type.matches(method.type().descriptor());
	}
}
