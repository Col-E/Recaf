package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX {@code const-method-handle} instruction and the structured handle payload.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param destination
 * 		Destination register matcher.
 * @param kind
 * 		Handle-kind matcher.
 * @param owner
 * 		Handle owner internal-name matcher.
 * @param name
 * 		Handle member-name matcher.
 * @param type
 * 		Handle member or method descriptor matcher.
 *
 * @author Matt Coley
 */
public record DexConstMethodHandleInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                                     @Nonnull Matcher<Integer> destination,
                                                     @Nonnull Matcher<Integer> kind,
                                                     @Nonnull Matcher<String> owner,
                                                     @Nonnull Matcher<String> name,
                                                     @Nonnull Matcher<String> type) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact opcode.
	 * @param destination
	 * 		Exact destination register.
	 * @param kind
	 * 		Exact handle kind.
	 * @param owner
	 * 		Exact owner internal name.
	 * @param name
	 * 		Exact member name.
	 * @param type
	 * 		Exact member or method descriptor.
	 *
	 * @return Matcher against the exact method-handle constant instruction.
	 */
	@Nonnull
	public static DexConstMethodHandleInstructionMatcher exact(int opcode, int destination, int kind,
	                                                           @Nonnull String owner, @Nonnull String name,
	                                                           @Nonnull String type) {
		return new DexConstMethodHandleInstructionMatcher(Matchers.exact(opcode), Matchers.exact(destination),
				Matchers.exact(kind), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	/**
	 * @param destination
	 * 		Exact destination register.
	 * @param kind
	 * 		Exact handle kind.
	 * @param owner
	 * 		Exact owner internal name.
	 * @param name
	 * 		Exact member name.
	 * @param type
	 * 		Exact member or method descriptor.
	 *
	 * @return Matcher against a method-handle constant with the given payload.
	 */
	@Nonnull
	public static DexConstMethodHandleInstructionMatcher exact(int destination, int kind,
	                                                           @Nonnull String owner, @Nonnull String name,
	                                                           @Nonnull String type) {
		return new DexConstMethodHandleInstructionMatcher(Matchers.any(), Matchers.exact(destination),
				Matchers.exact(kind), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(type));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		if (!(instruction instanceof ConstMethodHandleInstruction value)
				|| !opcode.matches(value.opcode())
				|| !destination.matches(value.destination()))
			return false;
		var handle = value.handle();
		return kind.matches(handle.kind())
				&& owner.matches(handle.owner().internalName())
				&& name.matches(handle.name())
				&& type.matches(handle.type().descriptor());
	}
}
