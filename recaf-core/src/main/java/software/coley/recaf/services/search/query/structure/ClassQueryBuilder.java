package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

import java.util.List;

/**
 * Fluent builder for {@link ClassQuery}.
 *
 * @author Matt Coley
 */
public final class ClassQueryBuilder {
	private Matcher<String> internalName = Matchers.any();
	private AccessFlagMatcher accessFlags = AccessFlagMatcher.any();
	private ListMatcher<AnnotationMatcher> annotations = ListMatcher.any();
	private ListMatcher<FieldMatcher> fields = ListMatcher.any();
	private ListMatcher<MethodMatcher> methods = ListMatcher.any();

	/**
	 * @param internalName
	 * 		Exact internal class name.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder name(@Nonnull String internalName) {
		this.internalName = Matchers.exact(internalName);
		return this;
	}

	/**
	 * @param internalName
	 * 		Internal class name matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder name(@Nonnull Matcher<String> internalName) {
		this.internalName = internalName;
		return this;
	}

	/**
	 * @param accessFlags
	 * 		Access flag constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder accessFlags(@Nonnull AccessFlagMatcher accessFlags) {
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
	public ClassQueryBuilder accessFlags(int first, int... additional) {
		this.accessFlags = AccessFlagMatcher.with(first, additional);
		return this;
	}

	/**
	 * @param annotations
	 * 		Annotation constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder annotations(@Nonnull ListMatcher<AnnotationMatcher> annotations) {
		this.annotations = annotations;
		return this;
	}

	/**
	 * @param annotations
	 * 		Annotations that must be present as an unordered bag.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder annotations(@Nonnull AnnotationMatcher... annotations) {
		this.annotations = ListMatcher.bag(List.of(annotations));
		return this;
	}

	/**
	 * @param fields
	 * 		Field constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder fields(@Nonnull ListMatcher<FieldMatcher> fields) {
		this.fields = fields;
		return this;
	}

	/**
	 * @param fields
	 * 		Fields that must be present as an unordered bag.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder fields(@Nonnull FieldMatcher... fields) {
		this.fields = ListMatcher.bag(List.of(fields));
		return this;
	}

	/**
	 * @param methods
	 * 		Method constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder methods(@Nonnull ListMatcher<MethodMatcher> methods) {
		this.methods = methods;
		return this;
	}

	/**
	 * @param methods
	 * 		Methods that must be present as an unordered bag.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ClassQueryBuilder methods(@Nonnull MethodMatcher... methods) {
		this.methods = ListMatcher.bag(List.of(methods));
		return this;
	}

	/**
	 * @return Immutable class query.
	 */
	@Nonnull
	public ClassQuery build() {
		return new ClassQuery(internalName, accessFlags, annotations, fields, methods);
	}
}
