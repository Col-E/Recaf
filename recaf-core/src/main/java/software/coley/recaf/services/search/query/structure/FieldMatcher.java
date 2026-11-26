package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;

/**
 * Matches field metadata.
 *
 * @param name
 * 		Field name matcher.
 * @param type
 * 		Field type matcher.
 * @param initialValue
 * 		Initial value matcher.
 * @param annotations
 * 		Annotation constraint.
 * @param accessFlags
 * 		Access constraint.
 *
 * @see MethodMatcher
 * @author Matt Coley
 */
public record FieldMatcher(@Nonnull Matcher<String> name,
                           @Nonnull Matcher<Type> type,
                           @Nonnull Matcher<Object> initialValue,
                           @Nonnull ListMatcher<AnnotationMatcher> annotations,
                           @Nonnull AccessFlagMatcher accessFlags) implements Matcher<FieldMember> {
	@Override
	public boolean matches(@Nullable FieldMember field) {
		// Reject missing members and cheap metadata mismatches before parsing descriptors.
		if (field == null || !name.matches(field.getName()) || !accessFlags.matches(field.getAccess()))
			return false;

		// Parse the descriptor only after the inexpensive checks have passed.
		try {
			return type.matches(Type.getType(field.getDescriptor())) &&
					initialValue.matches(field.getDefaultValue()) &&
					annotations.matches(field.getAnnotations());
		} catch (RuntimeException ex) {
			// Malformed candidate metadata is a non-match, not a workspace-wide search failure.
			return false;
		}
	}
}
