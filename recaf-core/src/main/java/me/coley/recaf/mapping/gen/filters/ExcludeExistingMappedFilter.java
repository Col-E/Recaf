package me.coley.recaf.mapping.gen.filters;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.AggregatedMappings;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;

/**
 * Filter that excludes names that have already been specified by {@link AggregatedMappings}.
 *
 * @author Matt Coley
 */
public class ExcludeExistingMappedFilter extends NameGeneratorFilter {
	private final AggregatedMappings aggregate;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param aggregate
	 * 		Aggregate mappings instance to use when checking for existing mapping entries.
	 */
	public ExcludeExistingMappedFilter(NameGeneratorFilter next, AggregatedMappings aggregate) {
		super(next, true);
		this.aggregate = aggregate;
	}

	@Override
	public boolean shouldMapClass(CommonClassInfo info) {
		if (aggregate.getReverseClassMapping(info.getName()) != null)
			return false;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		if (aggregate.getReverseFieldMapping(owner.getName(), info.getName(), info.getDescriptor()) != null)
			return false;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		if (aggregate.getReverseMethodMapping(owner.getName(), info.getName(), info.getDescriptor()) != null)
			return false;
		return super.shouldMapMethod(owner, info);
	}
}
