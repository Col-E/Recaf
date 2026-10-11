package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code instance-of} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param destination
 * 		Destination register matcher.
 * @param register
 * 		Object register matcher.
 * @param type
 * 		Class descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexInstanceOfInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                              @Nonnull Matcher<Integer> destination,
                                              @Nonnull Matcher<Integer> register,
                                              @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param destination
	 * 		Exact destination register.
	 * @param register
	 * 		Exact object register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against the exact instance-of instruction.
	 */
	@Nonnull
	public static DexInstanceOfInstructionMatcher exact(int opcode, int destination, int register,
	                                                    @Nonnull String type) {
		return new DexInstanceOfInstructionMatcher(Matchers.exact(opcode), Matchers.exact(destination),
				Matchers.exact(register), Matchers.exact(type));
	}

	/**
	 * @param destination
	 * 		Exact destination register.
	 * @param register
	 * 		Exact object register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against an instance-of with the given operands.
	 */
	@Nonnull
	public static DexInstanceOfInstructionMatcher exact(int destination, int register, @Nonnull String type) {
		return new DexInstanceOfInstructionMatcher(Matchers.any(), Matchers.exact(destination),
				Matchers.exact(register), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof InstanceOfInstruction instance
				&& opcode.matches(instance.opcode())
				&& destination.matches(instance.destination())
				&& register.matches(instance.register())
				&& type.matches(instance.type().descriptor());
	}
}
