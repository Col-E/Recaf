package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.search.AndroidClassSearchVisitor;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.query.AndroidClassQuery;
import software.coley.recaf.services.search.query.JvmClassQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.query.structure.android.AndroidClassStructureMatcher;
import software.coley.recaf.services.search.query.structure.jvm.JvmClassStructureMatcher;
import software.coley.recaf.services.search.result.ClassReference;

/**
 * Immutable class, field, method, and annotation structure query.
 *
 * @param internalName
 * 		Internal class-name matcher.
 * @param accessFlags
 * 		Class access constraint.
 * @param annotations
 * 		Class annotation constraint.
 * @param fields
 * 		Field constraint.
 * @param methods
 * 		Method constraint.
 *
 * @author Matt Coley
 */
public record ClassQuery(@Nonnull Matcher<String> internalName,
                         @Nonnull AccessFlagMatcher accessFlags,
                         @Nonnull ListMatcher<AnnotationMatcher> annotations,
                         @Nonnull ListMatcher<FieldMatcher> fields,
                         @Nonnull ListMatcher<MethodMatcher> methods) implements Query, JvmClassQuery, AndroidClassQuery {
	/**
	 * @param info
	 * 		Common class metadata.
	 *
	 * @return {@code true} when class metadata and fields match.
	 */
	public boolean matchesCommon(@Nonnull ClassInfo info) {
		return internalName.matches(info.getName()) && accessFlags.matches(info.getAccess()) &&
				annotations.matches(info.getAnnotations()) && fields.matches(info.getFields());
	}

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return (resultSink, classPath, classInfo) -> {
			// Preserve the existing visitor so structural queries compose with normal searches.
			if (delegate != null)
				delegate.visit(resultSink, classPath, classInfo);

			// Emit a class result only after the complete structure matches.
			if (JvmClassStructureMatcher.matches(this, classInfo))
				resultSink.accept(classPath, new ClassReference(classInfo.getName()));
		};
	}

	@Nonnull
	@Override
	public AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate) {
		return (resultSink, classPath, classInfo) -> {
			// Preserve the existing visitor so structural queries compose with normal searches.
			if (delegate != null)
				delegate.visit(resultSink, classPath, classInfo);

			// Emit a class result only after the complete structure matches.
			if (AndroidClassStructureMatcher.matches(this, classInfo))
				resultSink.accept(classPath, new ClassReference(classInfo.getName()));
		};
	}
}
