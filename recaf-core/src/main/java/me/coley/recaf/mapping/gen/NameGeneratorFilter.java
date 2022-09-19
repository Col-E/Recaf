package me.coley.recaf.mapping.gen;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;

/**
 * Base filter outline. An implementation of a filter would expand or limit the scope of the generated mappings.
 *
 * @author Matt Coley
 */
public abstract class NameGeneratorFilter {
	private final NameGeneratorFilter next;
	private final boolean defaultMap;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param defaultMap
	 *        {@code true} to make renaming things the default, treating chains as limitations on the baseline.
	 *        {@code false} to make keeping names the default, treating chains as expansions on the baseline.
	 */
	protected NameGeneratorFilter(NameGeneratorFilter next, boolean defaultMap) {
		this.next = next;
		this.defaultMap = defaultMap;
	}

	/**
	 * @return {@code true} to make renaming things the default, treating chains as limitations on the baseline.
	 * {@code false} to make keeping names the default, treating chains as expansions on the baseline.
	 */
	public boolean isDefaultMap() {
		return defaultMap;
	}

	/**
	 * @param info
	 * 		Class to check.
	 *
	 * @return {@code true} if the generator should create a new name for the class.
	 */
	public boolean shouldMapClass(CommonClassInfo info) {
		if (defaultMap)
			return next == null || next.shouldMapClass(info);
		else
			return next != null && next.shouldMapClass(info);
	}

	/**
	 * @param owner
	 * 		Class the field is defined in.
	 * @param info
	 * 		Field to check.
	 *
	 * @return {@code true} if the generator should create a new name for the field.
	 */
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		if (defaultMap)
			return next == null || next.shouldMapField(owner, info);
		else
			return next != null && next.shouldMapField(owner, info);
	}

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param info
	 * 		Method to check.
	 *
	 * @return {@code true} if the generator should create a new name for the method.
	 */
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		if (defaultMap)
			return next == null || next.shouldMapMethod(owner, info);
		else
			return next != null && next.shouldMapMethod(owner, info);
	}
}
