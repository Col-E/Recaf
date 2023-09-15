package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.gen.NameGeneratorFilter;
import software.coley.recaf.util.TextMatchMode;

/**
 * Filter that includes classes <i>(and their members)</i>.
 *
 * @author Matt Coley
 * @see ExcludeClassesFilter
 */
public class IncludeClassesFilter extends NameGeneratorFilter {
	private final String name;
	private final TextMatchMode matchMode;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param name
	 * 		Name pattern to exclude.
	 * @param matchMode
	 * 		Text match mode.
	 */
	public IncludeClassesFilter(@Nullable NameGeneratorFilter next,
								@Nonnull String name, @Nonnull TextMatchMode matchMode) {
		super(next, true);
		this.name = name;
		this.matchMode = matchMode;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		return super.shouldMapClass(info) &&
				(matchMode.match(this.name, info.getName()));
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
}
