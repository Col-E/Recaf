package me.coley.recaf.mapping.gen.filters;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;
import me.coley.recaf.util.AccessFlag;

import java.util.Collection;

/**
 * Filter that excludes classes and members that match the given access modifiers.
 *
 * @author Matt Coley
 * @see IncludeModifiersNameFilter
 */
public class ExcludeModifiersNameFilter extends NameGeneratorFilter {
	private final Collection<AccessFlag> flags;
	private final boolean targetClasses;
	private final boolean targetFields;
	private final boolean targetMethods;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param flags
	 * 		Access flags to check for.
	 * @param targetClasses
	 * 		Check against classes.
	 * @param targetFields
	 * 		Check against fields.
	 * @param targetMethods
	 * 		Check against methods.
	 */
	public ExcludeModifiersNameFilter(NameGeneratorFilter next, Collection<AccessFlag> flags,
									  boolean targetClasses, boolean targetFields, boolean targetMethods) {
		super(next, true);
		this.flags = flags;
		this.targetClasses = targetClasses;
		this.targetFields = targetFields;
		this.targetMethods = targetMethods;
	}

	@Override
	public boolean shouldMapClass(CommonClassInfo info) {
		if (targetClasses && AccessFlag.hasAny(info.getAccess(), flags))
			return false;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		if (targetFields && AccessFlag.hasAny(info.getAccess(), flags))
			return false;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		if (targetMethods && AccessFlag.hasAny(info.getAccess(), flags))
			return false;
		return super.shouldMapMethod(owner, info);
	}
}
