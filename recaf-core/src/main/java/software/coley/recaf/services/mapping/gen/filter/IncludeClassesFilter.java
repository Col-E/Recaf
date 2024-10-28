package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.search.match.StringPredicate;

/**
 * Filter that includes classes <i>(and their members)</i>.
 *
 * @author Matt Coley
 * @see ExcludeClassesFilter
 */
public class IncludeClassesFilter extends NameGeneratorFilter {
	private final StringPredicate namePredicate;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param namePredicate
	 * 		Class name predicate for included names.
	 */
	public IncludeClassesFilter(@Nullable NameGeneratorFilter next, @Nonnull StringPredicate namePredicate) {
		super(next, true);
		this.namePredicate = namePredicate;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		return super.shouldMapClass(info) &&
				(namePredicate.match(info.getName()));
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		// Consider owner type, we do not want to map fields if they are outside the inclusion filter
		return shouldMapClass(owner) && super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		// Consider owner type, we do not want to map methods if they are outside the inclusion filter
		return shouldMapClass(owner) && super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod,
	                                      @Nonnull LocalVariable variable) {
		// Consider owner type and method, we do not want to map variables if they are outside the inclusion filter
		return shouldMapClass(owner) && super.shouldMapMethod(owner, declaringMethod)
				&& super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
