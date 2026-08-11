package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;
import software.coley.recaf.util.Handles;

import java.util.Arrays;

/**
 * Matches an invokedynamic JVM instruction.
 *
 * @param name
 * 		Call-site name matcher.
 * @param descriptor
 * 		Call-site descriptor matcher.
 * @param bootstrapMethod
 * 		Bootstrap method matcher.
 * @param bootstrapArguments
 * 		Bootstrap argument matcher.
 *
 * @author Matt Coley
 */
public record JvmInvokeDynamicInsnNodeMatcher(@Nonnull Matcher<String> name,
                                              @Nonnull Matcher<String> descriptor,
                                              @Nonnull Matcher<Handle> bootstrapMethod,
                                              @Nonnull ListMatcher<Matcher<? extends Object>> bootstrapArguments) implements JvmInsnMatcher {
	/**
	 * @return Matcher for a call to {@code LambdaMetafactory#metafactory}.
	 */
	@Nonnull
	public static JvmInvokeDynamicInsnNodeMatcher metafactory() {
		return new JvmInvokeDynamicInsnNodeMatcher(
				Matchers.any(),
				Matchers.any(),
				Matchers.exact(Handles.META_FACTORY),
				ListMatcher.any()
		);
	}

	/**
	 * @param samType
	 * 		Single-abstract-method type, such as {@link java.util.function.Function}.
	 *
	 * @return Matcher for a call to {@code LambdaMetafactory#metafactory} with the given SAM type.
	 */
	@Nonnull
	public static JvmInvokeDynamicInsnNodeMatcher metafactory(@Nonnull Class<?> samType) {
		return metafactory(samType.getName().replace('.', '/'));
	}

	/**
	 * @param samInternalName
	 * 		Internal class name of the single-abstract-method type, such as {@code java/util/function/Function}.
	 *
	 * @return Matcher for a call to {@code LambdaMetafactory#metafactory} with the given SAM type.
	 */
	@Nonnull
	public static JvmInvokeDynamicInsnNodeMatcher metafactory(@Nonnull String samInternalName) {
		// The InvokeDynamicInsnNode will have:
		//  name: SAM method name
		//  desc: (receiverType)samType
		//   - The return type is what we're matching against.
		//  bsm: LambdaMetafactory.metafactory
		//  bsmArgs: [samMethodType, implMethodHandle, implMethodType]
		return new JvmInvokeDynamicInsnNodeMatcher(
				Matchers.any(),
				Matchers.endsWith(")L" + samInternalName + ";"),
				Matchers.exact(Handles.META_FACTORY),
				ListMatcher.any()
		);
	}

	/**
	 * @return Matcher for a call to {@code StringConcatFactory#makeConcatWithConstants}.
	 */
	@Nonnull
	public static JvmInvokeDynamicInsnNodeMatcher stringConcat() {
		return new JvmInvokeDynamicInsnNodeMatcher(
				Matchers.any(),
				Matchers.any(),
				Matchers.exact(Handles.STRING_CONCAT_FACTORY),
				ListMatcher.any()
		);
	}

	/**
	 * @param templateMatcher
	 * 		Matcher for the string template argument.
	 *
	 * @return Matcher for a call to {@code StringConcatFactory#makeConcatWithConstants}.
	 */
	@Nonnull
	public static JvmInvokeDynamicInsnNodeMatcher stringConcat(@Nonnull Matcher<String> templateMatcher) {
		// For the concat:  "Hello, " + user + "! You have " + count + " new messages."
		// The template is: "Hello, \u0001! You have \u0001 new messages."
		return new JvmInvokeDynamicInsnNodeMatcher(
				Matchers.any(),
				Matchers.any(),
				Matchers.exact(Handles.STRING_CONCAT_FACTORY),
				ListMatcher.exactly(templateMatcher)
		);
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		if (!(node instanceof InvokeDynamicInsnNode value)
				|| !name.matches(value.name)
				|| !descriptor.matches(value.desc)
				|| !bootstrapMethod.matches(value.bsm))
			return false;
		return bootstrapArguments.matches(Arrays.asList(value.bsmArgs));
	}
}
