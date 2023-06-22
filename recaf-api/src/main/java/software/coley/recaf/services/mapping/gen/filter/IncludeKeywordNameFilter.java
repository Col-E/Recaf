package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.gen.NameGeneratorFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter that includes names that are reserved Java keywords.
 *
 * @author Matt Coley
 */
public class IncludeKeywordNameFilter extends NameGeneratorFilter {
	private static final Set<String> keywords = new HashSet<>();

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeKeywordNameFilter(@Nonnull NameGeneratorFilter next) {
		super(next, false);
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (keywords.contains(info.getName()))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember info) {
		if (keywords.contains(info.getName()))
			return true;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember info) {
		if (keywords.contains(info.getName()))
			return true;
		return super.shouldMapMethod(owner, info);
	}

	static {
		keywords.addAll(Arrays.asList("abstract",
				"assert",
				"boolean",
				"break",
				"bridge",
				"byte",
				"case",
				"catch",
				"char",
				"class",
				"const",
				"continue",
				"default",
				"do",
				"double",
				"else",
				"enum",
				"extends",
				"final",
				"finally",
				"float",
				"for",
				"goto",
				"if",
				"implements",
				"import",
				"instanceof",
				"interface",
				"mandated",
				"module",
				"native",
				"open",
				"private",
				"protected",
				"public",
				"static",
				"strictfp",
				"super",
				"synchronized",
				"synthetic",
				"transient",
				"transitive",
				"var",
				"varargs",
				"volatile"));
	}
}
