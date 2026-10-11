package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code const-class} instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param register
 * 		Destination register matcher.
 * @param type
 * 		Class descriptor matcher, such as {@code Ljava/lang/String;} or {@code I}.
 *
 * @author Matt Coley
 */
public record DexConstTypeInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                             @Nonnull Matcher<Integer> register,
                                             @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param register
	 * 		Exact destination register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against the exact class constant instruction.
	 */
	@Nonnull
	public static DexConstTypeInstructionMatcher exact(int opcode, int register, @Nonnull String type) {
		return new DexConstTypeInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(type));
	}

	/**
	 * @param register
	 * 		Exact destination register.
	 * @param type
	 * 		Exact class descriptor.
	 *
	 * @return Matcher against a class constant with the given operands.
	 */
	@Nonnull
	public static DexConstTypeInstructionMatcher exact(int register, @Nonnull String type) {
		return new DexConstTypeInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof ConstTypeInstruction typeInsn
				&& opcode.matches(typeInsn.opcode())
				&& register.matches(typeInsn.register())
				&& type.matches(typeInsn.type().descriptor());
	}
}
