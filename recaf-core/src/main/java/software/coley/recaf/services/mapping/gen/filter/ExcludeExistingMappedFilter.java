package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;

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
	public ExcludeExistingMappedFilter(@Nullable NameGeneratorFilter next, @Nonnull AggregatedMappings aggregate) {
		super(next, true);
		this.aggregate = aggregate;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (aggregate.getReverseClassMapping(info.getName()) != null)
			return false;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (aggregate.getReverseFieldMapping(owner.getName(), field.getName(), field.getDescriptor()) != null)
			return false;
		return super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (aggregate.getReverseMethodMapping(owner.getName(), method.getName(), method.getDescriptor()) != null)
			return false;
		return super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (aggregate.getReverseVariableMapping(owner.getName(), declaringMethod.getName(),
				declaringMethod.getDescriptor(), variable.getName(), variable.getDescriptor(), variable.getIndex()) != null)
			return false;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
