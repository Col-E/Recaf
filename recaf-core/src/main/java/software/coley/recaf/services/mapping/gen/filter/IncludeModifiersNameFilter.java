package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;

/**
 * Filter that includes classes and members that match the given access modifiers.
 *
 * @author Matt Coley
 * @see ExcludeModifiersNameFilter
 */
public class IncludeModifiersNameFilter extends NameGeneratorFilter {
	private final int[] flags;
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
	public IncludeModifiersNameFilter(@Nullable NameGeneratorFilter next, @Nonnull Collection<Integer> flags,
									  boolean targetClasses, boolean targetFields, boolean targetMethods) {
		super(next, false);
		this.flags = flags.stream().mapToInt(i -> i).toArray();
		this.targetClasses = targetClasses;
		this.targetFields = targetFields;
		this.targetMethods = targetMethods;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (targetClasses && info.hasAnyModifiers(flags))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (targetFields && field.hasAnyModifiers(flags))
			return true;
		return super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (targetMethods && method.hasAnyModifiers(flags))
			return true;
		return super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		// Variables are not targeted, so delegate to next filter
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
