package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.StringUtil;

import java.util.List;
import java.util.Set;

import static software.coley.recaf.util.Keywords.getKeywords;

/**
 * Filter that includes names that contain <i>(when split by boundary characters)</i> reserved Java keywords.
 *
 * @author Matt Coley
 */
public class IncludeKeywordNameFilter extends NameGeneratorFilter {
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
		if (containsKeyword(name))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember info) {
		if (containsKeyword(info.getName()))
			return true;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember info) {
		if (containsKeyword(info.getName()))
			return true;
		return super.shouldMapMethod(owner, info);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		// Edge case: 'this' is allowed only as local variable slot 0 on non-static methods.
		if (!declaringMethod.hasStaticModifier() && variable.getIndex() == 0 && "this".equals(variable.getName()))
			return false;
		if (containsKeyword(variable.getName()))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}

	private static boolean containsKeyword(@Nonnull String name) {
		Set<String> keywords = getKeywords();
		List<String> parts = StringUtil.fastSplitNonIdentifier(name);
		for (String part : parts) {
			if (keywords.contains(part))
				return true;
		}
		return false;
	}
}
