package me.coley.recaf.mapping.gen.filters;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;

/**
 * Filter that includes names that are outside the standard ASCII range used for normal class/member names.
 *
 * @author Matt Coley
 */
public class IncludeNonAsciiFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeNonAsciiFilter(NameGeneratorFilter next) {
		super(next, false);
	}

	private static boolean shouldMap(ItemInfo info) {
		String name = info.getName();
		return name.codePoints()
				.anyMatch(code -> (code < 0x21 || code > 0x7A));
	}

	@Override
	public boolean shouldMapClass(CommonClassInfo info) {
		if (shouldMap(info))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		if (shouldMap(info))
			return true;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		if (shouldMap(info))
			return true;
		return super.shouldMapMethod(owner, info);
	}
}
