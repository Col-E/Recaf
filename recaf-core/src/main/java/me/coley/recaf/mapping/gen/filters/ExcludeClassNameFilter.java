package me.coley.recaf.mapping.gen.filters;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;
import me.coley.recaf.search.TextMatchMode;

/**
 * Filter that excludes classes <i>(and their members)</i> that match the given path.
 *
 * @author Matt Coley
 * @see IncludeClassNameFilter
 */
public class ExcludeClassNameFilter extends NameGeneratorFilter {
	private final String path;
	private final TextMatchMode matchMode;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param path
	 * 		Class path name to exclude.
	 * @param matchMode
	 * 		Text match mode.
	 */
	public ExcludeClassNameFilter(NameGeneratorFilter next, String path, TextMatchMode matchMode) {
		super(next, true);
		this.path = path;
		this.matchMode = matchMode;
	}

	@Override
	public boolean shouldMapClass(CommonClassInfo info) {
		String name = info.getName();
		boolean matches = matchMode.match(path, name);
		if (matches)
			return false; // (exclusion) class name contains blacklisted path
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		// Consider owner type, we do not want to map fields if they are inside the exclusion filter
		return super.shouldMapField(owner, info) && shouldMapClass(owner);
	}

	@Override
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		// Consider owner type, we do not want to map methods if they are inside the exclusion filter
		return super.shouldMapMethod(owner, info) && shouldMapClass(owner);
	}
}
