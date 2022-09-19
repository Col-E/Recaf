package me.coley.recaf.mapping.gen.filters;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;
import me.coley.recaf.util.EscapeUtil;

/**
 * Filter that includes names that contain whitespaces, which are illegal in standard Java source.
 *
 * @author Matt Coley
 */
public class IncludeWhitespacesFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeWhitespacesFilter(NameGeneratorFilter next) {
		super(next, false);
	}

	private static boolean shouldMap(ItemInfo info) {
		String name = info.getName();
		return EscapeUtil.containsWhitespace(name);
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
