package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link MethodMatcher}.
 *
 * @author Matt Coley
 */
public final class MethodMatcherBuilder {
	private Matcher<String> name = Matchers.any();
	private Matcher<Type> returnType = Matchers.any();
	private AccessFlagMatcher accessFlags = AccessFlagMatcher.any();
	private ListMatcher<Matcher<Type>> parameterTypes = ListMatcher.any();
	private ListMatcher<AnnotationMatcher> annotations = ListMatcher.any();
	private ListMatcher<Matcher<Type>> exceptions = ListMatcher.any();
	private ListMatcher<TryCatchBlockMatcher> tryCatchBlocks = ListMatcher.any();
	private ListMatcher<InsnMatcher> instructions = ListMatcher.any();

	/**
	 * @param name
	 * 		Exact method name.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder name(@Nonnull String name) {
		this.name = Matchers.exact(name);
		return this;
	}

	/**
	 * @param name
	 * 		Method name matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder name(@Nonnull Matcher<String> name) {
		this.name = name;
		return this;
	}

	/**
	 * @param returnType
	 * 		Exact method return type.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder returnType(@Nonnull Type returnType) {
		this.returnType = Matchers.exact(returnType);
		return this;
	}

	/**
	 * @param returnType
	 * 		Method return type matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder returnType(@Nonnull Matcher<Type> returnType) {
		this.returnType = returnType;
		return this;
	}

	/**
	 * @param accessFlags
	 * 		Access flag constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder accessFlags(@Nonnull AccessFlagMatcher accessFlags) {
		this.accessFlags = accessFlags;
		return this;
	}

	/**
	 * @param first
	 * 		First required access flag.
	 * @param additional
	 * 		Additional required access flags.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder accessFlags(int first, int... additional) {
		this.accessFlags = AccessFlagMatcher.with(first, additional);
		return this;
	}

	/**
	 * @param parameterTypes
	 * 		Exact method parameter types in declaration order.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder exactParameters(@Nonnull Type... parameterTypes) {
		List<Matcher<Type>> matchers = new ArrayList<>(parameterTypes.length);
		for (Type parameterType : parameterTypes)
			matchers.add(Matchers.exact(parameterType));
		this.parameterTypes = ListMatcher.sequence(matchers);
		return this;
	}

	/**
	 * @param parameters
	 * 		Parameter type constraint in declaration order.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder parameters(@Nonnull ListMatcher<Matcher<Type>> parameters) {
		this.parameterTypes = parameters;
		return this;
	}

	/**
	 * @param annotations
	 * 		Annotation constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder annotations(@Nonnull ListMatcher<AnnotationMatcher> annotations) {
		this.annotations = annotations;
		return this;
	}

	/**
	 * @param exceptions
	 * 		Thrown exception type constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder exceptions(@Nonnull ListMatcher<Matcher<Type>> exceptions) {
		this.exceptions = exceptions;
		return this;
	}

	/**
	 * @param tryCatchBlocks
	 * 		Try/catch block constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder tryCatchBlocks(@Nonnull ListMatcher<TryCatchBlockMatcher> tryCatchBlocks) {
		this.tryCatchBlocks = tryCatchBlocks;
		return this;
	}

	/**
	 * @param instructions
	 * 		Instruction matchers in declaration order.
	 * @param count
	 * 		Constraint over the number of consumed instructions.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder instructions(@Nonnull List<InsnMatcher> instructions, @Nonnull CountConstraint count) {
		this.instructions = ListMatcher.sequence(instructions, count);
		return this;
	}

	/**
	 * @param instructions
	 * 		Exact-size instruction sequence.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public MethodMatcherBuilder instructions(@Nonnull InsnMatcher... instructions) {
		this.instructions = ListMatcher.sequence(List.of(instructions));
		return this;
	}

	/**
	 * @return Immutable method matcher.
	 */
	@Nonnull
	public MethodMatcher build() {
		return new MethodMatcher(name, returnType, accessFlags, parameterTypes, annotations, exceptions, tryCatchBlocks, instructions);
	}
}
