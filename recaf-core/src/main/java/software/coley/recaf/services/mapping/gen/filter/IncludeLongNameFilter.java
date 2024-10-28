package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

/**
 * Filter that includes names that are longer than a given size.
 *
 * @author Matt Coley
 */
public class IncludeLongNameFilter extends NameGeneratorFilter {
	private final int maxNameLength;
	private final boolean targetClasses;
	private final boolean targetFields;
	private final boolean targetMethods;
	private final boolean targetVariables;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param maxNameLength
	 * 		Max length of names allowed.
	 * @param targetClasses
	 * 		Check against classes.
	 * @param targetFields
	 * 		Check against fields.
	 * @param targetMethods
	 * 		Check against methods.
	 * @param targetVariables
	 * 		Check against variables.
	 */
	public IncludeLongNameFilter(@Nullable NameGeneratorFilter next, int maxNameLength,
	                             boolean targetClasses, boolean targetFields, boolean targetMethods, boolean targetVariables) {
		super(next, false);
		this.maxNameLength = maxNameLength;
		this.targetClasses = targetClasses;
		this.targetFields = targetFields;
		this.targetMethods = targetMethods;
		this.targetVariables = targetVariables;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (targetClasses && shouldMap(info))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (targetFields && shouldMap(field))
			return true;
		return super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (targetMethods && shouldMap(method))
			return true;
		return super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (targetVariables && shouldMap(variable))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}

	private boolean shouldMap(ClassInfo info) {
		return shouldMap(info.getName());
	}

	private boolean shouldMap(ClassMember info) {
		return shouldMap(info.getName());
	}

	private boolean shouldMap(LocalVariable info) {
		return shouldMap(info.getName());
	}

	private boolean shouldMap(String name) {
		return name.length() > maxNameLength;
	}
}
