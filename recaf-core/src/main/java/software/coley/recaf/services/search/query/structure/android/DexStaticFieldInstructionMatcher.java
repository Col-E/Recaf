package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX static-field instruction and its field reference.
 *
 * @param opcode
 * 		Opcode matcher. This distinguishes get/put and the field value width/kind.
 * @param kind
 * 		Field-kind offset matcher as represented by the DEX model.
 * @param value
 * 		Register carrying or receiving the field value.
 * @param owner
 * 		Field owner internal-name matcher.
 * @param name
 * 		Field name matcher.
 * @param type
 * 		Field descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexStaticFieldInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                               @Nonnull Matcher<Integer> kind,
                                               @Nonnull Matcher<Integer> value,
                                               @Nonnull Matcher<String> owner,
                                               @Nonnull Matcher<String> name,
                                               @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact static-field opcode.
	 * @param value
	 * 		Exact register carrying or receiving the field value.
	 * @param owner
	 * 		Exact field owner internal name.
	 * @param name
	 * 		Exact field name.
	 * @param type
	 * 		Exact field descriptor.
	 *
	 * @return Matcher against the exact static-field instruction.
	 */
	@Nonnull
	public static DexStaticFieldInstructionMatcher exact(int opcode, int value,
	                                                     @Nonnull String owner, @Nonnull String name,
	                                                     @Nonnull String type) {
		return new DexStaticFieldInstructionMatcher(Matchers.exact(opcode), Matchers.any(), Matchers.exact(value),
				Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	/**
	 * @param value
	 * 		Exact register carrying or receiving the field value.
	 * @param owner
	 * 		Exact field owner internal name.
	 * @param name
	 * 		Exact field name.
	 * @param type
	 * 		Exact field descriptor.
	 *
	 * @return Matcher against a static-field instruction with the given operands.
	 */
	@Nonnull
	public static DexStaticFieldInstructionMatcher exact(int value,
	                                                     @Nonnull String owner, @Nonnull String name,
	                                                     @Nonnull String type) {
		return new DexStaticFieldInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.exact(value),
				Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof StaticFieldInstruction field
				&& opcode.matches(field.opcode())
				&& kind.matches(field.kind())
				&& value.matches(field.value())
				&& owner.matches(field.owner().internalName())
				&& name.matches(field.name())
				&& type.matches(field.type().descriptor());
	}
}
