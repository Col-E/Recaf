package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static software.coley.recaf.util.Keywords.getKeywords;

/**
 * Filter that includes names that are reserved Java keywords.
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
		List<String> parts = StringUtil.fastSplit(name, true, '/');
		for (String part : parts)
			if (getKeywords().contains(part))
				return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember info) {
		if (getKeywords().contains(info.getName()))
			return true;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember info) {
		if (getKeywords().contains(info.getName()))
			return true;
		return super.shouldMapMethod(owner, info);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (getKeywords().contains(variable.getName()))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
