package software.coley.recaf.services.mapping.gen.filter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.gen.NameGeneratorFilter;

/**
 * Filter to prevent renaming of {@code Enum.values()} and {@code Enum.valueOf(String)} implementations.
 *
 * @author Matt Coley
 */
public class ExcludeEnumMethodsFilter extends NameGeneratorFilter {
	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	public ExcludeEnumMethodsFilter(@Nullable NameGeneratorFilter next) {
		super(next, true);
	}

	@Override
	public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		if (owner.hasEnumModifier()) {
			String ownerName = owner.getName();
			String name = method.getName();
			String desc = method.getDescriptor();
			if (name.equals("values") && desc.equals("()[L" + ownerName + ";")) {
				return false;
			} else if (name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + ownerName + ";")) {
				return false;
			}
		}
		return super.shouldMapMethod(owner, method);
	}
}
