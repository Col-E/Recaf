package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

import java.util.List;

/**
 * Fluent builder for {@link FieldMatcher}.
 *
 * @author Matt Coley
 */
public final class FieldMatcherBuilder {
	private Matcher<String> name = Matchers.any();
	private Matcher<Type> type = Matchers.any();
	private Matcher<Object> initialValue = Matchers.any();
	private ListMatcher<AnnotationMatcher> annotations = ListMatcher.any();
	private AccessFlagMatcher accessFlags = AccessFlagMatcher.any();

	/**
	 * @param name
	 * 		Exact field name.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder name(@Nonnull String name) {
		this.name = Matchers.exact(name);
		return this;
	}

	/**
	 * @param name
	 * 		Field name matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder name(@Nonnull Matcher<String> name) {
		this.name = name;
		return this;
	}

	/**
	 * @param type
	 * 		Exact field type.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder type(@Nonnull Type type) {
		this.type = Matchers.exact(type);
		return this;
	}

	/**
	 * @param type
	 * 		Field type matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder type(@Nonnull Matcher<Type> type) {
		this.type = type;
		return this;
	}

	/**
	 * @param value
	 * 		Exact initial value, including {@code null}.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder initialValue(@Nullable Object value) {
		this.initialValue = Matchers.exact(value);
		return this;
	}

	/**
	 * @param value
	 * 		Initial value matcher.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder initialValue(@Nonnull Matcher<Object> value) {
		this.initialValue = value;
		return this;
	}

	/**
	 * @param annotations
	 * 		Annotation constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder annotations(@Nonnull ListMatcher<AnnotationMatcher> annotations) {
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
	public FieldMatcherBuilder annotations(@Nonnull AnnotationMatcher... annotations) {
		this.annotations = ListMatcher.bag(List.of(annotations));
		return this;
	}

	/**
	 * @param accessFlags
	 * 		Access flag constraint.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public FieldMatcherBuilder accessFlags(@Nonnull AccessFlagMatcher accessFlags) {
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
	public FieldMatcherBuilder accessFlags(int first, int... additional) {
		this.accessFlags = AccessFlagMatcher.with(first, additional);
		return this;
	}

	/**
	 * @return Immutable field matcher.
	 */
	@Nonnull
	public FieldMatcher build() {
		return new FieldMatcher(name, type, initialValue, annotations, accessFlags);
	}
}
