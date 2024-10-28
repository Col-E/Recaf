package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

/**
 * Filter that includes names that are outside the standard ASCII range used for normal class/member names.
 *
 * @author Matt Coley
 */
public class IncludeNonAsciiNameFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeNonAsciiNameFilter(@Nullable NameGeneratorFilter next) {
		super(next, false);
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		if (shouldMap(info))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		if (shouldMap(field))
			return true;
		return super.shouldMapField(owner, field);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (shouldMap(method))
			return true;
		return super.shouldMapMethod(owner, method);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (shouldMap(variable))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}

	private static boolean shouldMap(ClassInfo info) {
		return shouldMap(info.getName());
	}

	private static boolean shouldMap(ClassMember info) {
		return shouldMap(info.getName());
	}

	private static boolean shouldMap(LocalVariable info) {
		return shouldMap(info.getName());
	}

	private static boolean shouldMap(String name) {
		return name.codePoints()
				.anyMatch(code -> (code < 0x21 || code > 0x7A) || Character.isWhitespace(code));
	}
}
