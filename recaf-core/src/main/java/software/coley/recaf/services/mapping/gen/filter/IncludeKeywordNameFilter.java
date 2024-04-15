package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.StringUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter that includes names that are reserved Java keywords.
 *
 * @author Matt Coley
 */
public class IncludeKeywordNameFilter extends NameGeneratorFilter {
	public static final Set<String> keywords = new HashSet<>();

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeKeywordNameFilter(@Nullable NameGeneratorFilter next) {
		super(next, false);
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		String name = info.getName();
		List<String> parts = StringUtil.fastSplit(name, true, '/');
		for (String part : parts)
			if (keywords.contains(part))
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
		// Commented out items are 'keywords' but can be used as names.
		keywords.addAll(Arrays.asList("abstract",
				"assert",
				"boolean",
				"break",
				// "bridge",
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
				// "mandated",
				// "module",
				"native",
				// "open",
				"private",
				"protected",
				"public",
				"static",
				"strictfp",
				"super",
				"synchronized",
				// "synthetic",
				"transient",
				"throws",
				// "transitive",
				// "var",
				// "varargs",
				"volatile",
				// primitives
				"boolean",
				"byte",
				"char",
				"short",
				"int",
				"long",
				"float",
				"double",
				"void",
				"null"
		));
	}
}
