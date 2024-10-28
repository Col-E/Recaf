package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.EscapeUtil;

/**
 * Filter that includes names that contain whitespaces, which are illegal in standard Java source.
 *
 * @author Matt Coley
 */
public class IncludeWhitespaceNameFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeWhitespaceNameFilter(@Nullable NameGeneratorFilter next) {
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

	private static boolean shouldMap(ClassMember member) {
		return shouldMap(member.getName());
	}

	private static boolean shouldMap(LocalVariable variable) {
		return shouldMap(variable.getName());
	}

	private static boolean shouldMap(String name) {
		return EscapeUtil.containsWhitespace(name);
	}
}
