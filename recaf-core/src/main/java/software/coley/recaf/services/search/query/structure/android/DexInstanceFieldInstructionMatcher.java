package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX instance-field instruction and its field reference.
 *
 * @param opcode
 * 		Opcode matcher. This distinguishes get/put and the field value width/kind.
 * @param kind
 * 		Field-kind offset matcher as represented by the DEX model.
 * @param value
 * 		Register carrying or receiving the field value.
 * @param instance
 * 		Register containing the field owner object.
 * @param owner
 * 		Field owner internal-name matcher.
 * @param name
 * 		Field name matcher.
 * @param type
 * 		Field descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexInstanceFieldInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                 @Nonnull Matcher<Integer> kind,
                                                 @Nonnull Matcher<Integer> value,
                                                 @Nonnull Matcher<Integer> instance,
                                                 @Nonnull Matcher<String> owner,
                                                 @Nonnull Matcher<String> name,
                                                 @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact instance-field opcode.
	 * @param value
	 * 		Exact register carrying or receiving the field value.
	 * @param instance
	 * 		Exact register containing the field owner object.
	 * @param owner
	 * 		Exact field owner internal name.
	 * @param name
	 * 		Exact field name.
	 * @param type
	 * 		Exact field descriptor.
	 *
	 * @return Matcher against the exact instance-field instruction.
	 */
	@Nonnull
	public static DexInstanceFieldInstructionMatcher exact(int opcode, int value, int instance,
	                                                       @Nonnull String owner, @Nonnull String name,
	                                                       @Nonnull String type) {
		return new DexInstanceFieldInstructionMatcher(Matchers.exact(opcode), Matchers.any(), Matchers.exact(value),
				Matchers.exact(instance), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	/**
	 * @param value
	 * 		Exact register carrying or receiving the field value.
	 * @param instance
	 * 		Exact register containing the field owner object.
	 * @param owner
	 * 		Exact field owner internal name.
	 * @param name
	 * 		Exact field name.
	 * @param type
	 * 		Exact field descriptor.
	 *
	 * @return Matcher against an instance-field instruction with the given operands.
	 */
	@Nonnull
	public static DexInstanceFieldInstructionMatcher exact(int value, int instance,
	                                                       @Nonnull String owner, @Nonnull String name,
	                                                       @Nonnull String type) {
		return new DexInstanceFieldInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.exact(value),
				Matchers.exact(instance), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof InstanceFieldInstruction field
				&& opcode.matches(field.opcode())
				&& kind.matches(field.kind())
				&& value.matches(field.value())
				&& instance.matches(field.instance())
				&& owner.matches(field.owner().internalName())
				&& name.matches(field.name())
				&& type.matches(field.type().descriptor());
	}
}
