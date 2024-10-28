package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

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
	protected NameGeneratorFilter(@Nullable NameGeneratorFilter next, boolean defaultMap) {
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
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (defaultMap)
			return next == null || next.shouldMapClass(info);
		else
			return next != null && next.shouldMapClass(info);
	}

	/**
	 * @param owner
	 * 		Class the field is defined in.
	 * @param field
	 * 		Field to check.
	 *
	 * @return {@code true} if the generator should create a new name for the field.
	 */
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (defaultMap)
			return next == null || next.shouldMapField(owner, field);
		else
			return next != null && next.shouldMapField(owner, field);
	}

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} if the generator should create a new name for the method.
	 */
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (defaultMap)
			return next == null || next.shouldMapMethod(owner, method);
		else
			return next != null && next.shouldMapMethod(owner, method);
	}

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param declaringMethod
	 * 		Method the variable is defined in.
	 * @param variable Variable to check.
	 *
	 * @return {@code true} if the generator should create a new name for the method.
	 */
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (defaultMap)
			return next == null || next.shouldMapLocalVariable(owner, declaringMethod, variable);
		else
			return next != null && next.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
