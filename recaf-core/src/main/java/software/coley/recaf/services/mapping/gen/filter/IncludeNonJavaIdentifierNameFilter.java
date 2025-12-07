package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.StringUtil;

import java.util.List;
import java.util.Set;

/**
 * Filter that includes names that do not comply with {@link Character#isJavaIdentifierStart(char)} and {@link Character#isJavaIdentifierPart(char)}.
 *
 * @author Matt Coley
 */
public class IncludeNonJavaIdentifierNameFilter extends NameGeneratorFilter {
	private static final Set<String> classExemptions = Set.of("package-info", "module-info");

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public IncludeNonJavaIdentifierNameFilter(@Nullable NameGeneratorFilter next) {
		super(next, false);
	}

	@Override
	public boolean shouldMapClass(@Nonnull ClassInfo info) {
		String name = info.getName();

		// Filter out package/module-info classes
		if (name.endsWith("package-info"))
			name = name.substring(0, name.length() - "package-info".length());
		else if (name.endsWith("module-info"))
			name = name.substring(0, name.length() - "module-info".length());

		if (isInvalidName(name))
			return true;
		return super.shouldMapClass(info);
	}

	@Override
	public boolean shouldMapField(@Nonnull ClassInfo owner, @Nonnull FieldMember info) {
		if (isInvalidName(info.getName()))
			return true;
		return super.shouldMapField(owner, info);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember info) {
		if (isInvalidName(info.getName()))
			return true;
		return super.shouldMapMethod(owner, info);
	}

	@Override
	public boolean shouldMapLocalVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
		if (isInvalidName(variable.getName()))
			return true;
		return super.shouldMapLocalVariable(owner, declaringMethod, variable);
	}

	private static boolean isInvalidName(@Nonnull String name) {
		List<String> parts = StringUtil.fastSplitNonIdentifier(name);
		for (String part : parts) {
			int length = part.length();
			if (length == 0)
				return true;
			else if (length == 1)
				return !Character.isJavaIdentifierStart(part.charAt(0));
			else {
				char[] chars = part.toCharArray();
				if (!Character.isJavaIdentifierStart(chars[0]))
					return true;
				for (int i = 1; i < chars.length; i++) {
					if (!Character.isJavaIdentifierPart(chars[i]))
						return true;
				}
			}
		}
		return false;
	}
}
