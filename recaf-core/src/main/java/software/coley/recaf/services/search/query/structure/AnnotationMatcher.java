package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches an annotation by descriptor.
 *
 * @param descriptor
 * 		Descriptor matcher.
 *
 * @author Matt Coley
 */
public record AnnotationMatcher(@Nonnull Matcher<String> descriptor) implements Matcher<AnnotationInfo> {
	/**
	 * @param descriptor
	 * 		Annotation descriptor to match.
	 *
	 * @return Matcher against the exact descriptor.
	 */
	@Nonnull
	public static AnnotationMatcher exact(@Nonnull String descriptor) {
		return new AnnotationMatcher(Matchers.exact(descriptor));
	}

	@Override
	public boolean matches(@Nullable AnnotationInfo annotation) {
		return annotation != null && descriptor.matches(annotation.getDescriptor());
	}
}
