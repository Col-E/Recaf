package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.search.match.StringPredicate;

/**
 * Filter that includes classes, fields, and methods by their names.
 *
 * @author Matt Coley
 * @see ExcludeNameFilter
 */
public class IncludeNameFilter extends NameGeneratorFilter {
	private final StringPredicate classPredicate;
	private final StringPredicate fieldPredicate;
	private final StringPredicate methodPredicate;
	private final StringPredicate variablePredicate;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 * @param classPredicate
	 * 		Class name predicate for excluded names.
	 *        {@code null} to skip filtering for class names.
	 * @param fieldPredicate
	 * 		Field name predicate for excluded names.
	 *        {@code null} to skip filtering for field names.
	 * @param methodPredicate
	 * 		Method name predicate for excluded names.
	 *        {@code null} to skip filtering for method names.
	 * @param variablePredicate
	 * 		Variable name predicate for excluded names.
	 *        {@code null} to skip filtering for variable names.
	 */
	public IncludeNameFilter(@Nullable NameGeneratorFilter next, @Nullable StringPredicate classPredicate,
							 @Nullable StringPredicate fieldPredicate, @Nullable StringPredicate methodPredicate,
							 @Nullable StringPredicate variablePredicate) {
		super(next, false);
		this.classPredicate = classPredicate;
		this.fieldPredicate = fieldPredicate;
		this.methodPredicate = methodPredicate;
		this.variablePredicate = variablePredicate;
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (classPredicate != null && classPredicate.match(info.getName()))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (fieldPredicate != null && fieldPredicate.match(field.getName()))
			return true;
		return super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (methodPredicate != null && methodPredicate.match(method.getName()))
			return true;
		return super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (variablePredicate != null && variablePredicate.match(variable.getName()))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}
}
