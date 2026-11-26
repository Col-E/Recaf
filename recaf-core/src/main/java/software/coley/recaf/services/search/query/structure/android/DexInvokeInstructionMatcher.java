package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;

/**
 * Matches a native DEX method invocation.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param owner
 * 		Method owner matcher.
 * @param name
 * 		Method name matcher.
 * @param descriptor
 * 		Call-site method descriptor matcher. For invoke-polymorphic, constrain the referenced method's declaration
 * 		with {@code methodDescriptor}.
 * @param arguments
 * 		Argument register matcher, in invocation order.
 * @param isRange
 * 		Matcher for whether the invocation uses the DEX range encoding.
 * @param methodDescriptor
 * 		Descriptor matcher for the referenced method prototype. For invoke-polymorphic, this differs from
 * 		the call-site {@code descriptor}.
 *
 * @author Matt Coley
 */
public record DexInvokeInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                          @Nonnull Matcher<String> owner,
                                          @Nonnull Matcher<String> name,
                                          @Nonnull Matcher<String> descriptor,
                                          @Nonnull ListMatcher<Matcher<Integer>> arguments,
                                          @Nonnull Matcher<Boolean> isRange,
                                          @Nonnull Matcher<String> methodDescriptor) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Canonical DEX invoke opcode.
	 * @param owner
	 * 		Internal owner name.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Call-site method descriptor.
	 *
	 * @return Matcher for the owner, name, and call-site prototype, accepting either register encoding. The declared
	 * method prototype is unconstrained; use the overload with {@code methodDescriptor} to constrain it.
	 */
	@Nonnull
	public static DexInvokeInstructionMatcher exact(int opcode, @Nonnull String owner,
	                                                @Nonnull String name, @Nonnull String descriptor) {
		return new DexInvokeInstructionMatcher(Matchers.exact(opcode), Matchers.exact(owner), Matchers.exact(name),
				Matchers.exact(descriptor), ListMatcher.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param opcode
	 * 		Canonical DEX invoke opcode.
	 * @param owner
	 * 		Internal owner name.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Call-site method descriptor.
	 * @param methodDescriptor
	 * 		Referenced method prototype descriptor.
	 *
	 * @return Matcher for the exact owner, method name, call-site prototype, and declared method prototype.
	 */
	@Nonnull
	public static DexInvokeInstructionMatcher exact(int opcode, @Nonnull String owner, @Nonnull String name,
	                                                @Nonnull String descriptor, @Nonnull String methodDescriptor) {
		return new DexInvokeInstructionMatcher(Matchers.exact(opcode), Matchers.exact(owner), Matchers.exact(name),
				Matchers.exact(descriptor), ListMatcher.any(), Matchers.any(), Matchers.exact(methodDescriptor));
	}

	/**
	 * @return Matcher for any DEX invocation using the range register encoding.
	 */
	@Nonnull
	public static DexInvokeInstructionMatcher range() {
		return new DexInvokeInstructionMatcher(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				ListMatcher.any(), Matchers.exact(true), Matchers.any());
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		if (!(instruction instanceof InvokeInstruction invoke)
				|| !opcode.matches(invoke.opcode())
				|| !owner.matches(invoke.owner().internalName())
				|| !name.matches(invoke.name())
				|| !descriptor.matches(invoke.type().descriptor())
				|| !methodDescriptor.matches(invoke.methodType().descriptor())
				|| !isRange.matches(invoke.isRange()))
			return false;
		return arguments.matches(DexRegisterList.of(invoke));
	}
}
