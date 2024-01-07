package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;

/**
 * Predicate to use in {@link MemberFilteringVisitor} and {@link MemberRemovingVisitor}.
 * <br
 * Has a default always-false field matching implementation to facilitate SAM usage for methods.
 *
 * @author Matt Coley
 * @see FieldPredicate
 */
public interface MethodPredicate extends MemberPredicate {
	/**
	 * @param method
	 * 		Method to match.
	 *
	 * @return Predicate matching a single method.
	 */
	@Nonnull
	static MethodPredicate of(@Nonnull MethodMember method) {
		return (access, name, desc, sig, exceptions) -> method.getName().equals(name) && method.getDescriptor().equals(desc);
	}

	/**
	 * @param methods
	 * 		Methods to match.
	 *
	 * @return Predicate matching a collection of methods.
	 */
	@Nonnull
	static MethodPredicate of(@Nonnull Collection<MethodMember> methods) {
		return (access, name, desc, sig, exceptions) -> {
			for (MethodMember method : methods)
				if (method.getName().equals(name) && method.getDescriptor().equals(desc))
					return true;
			return false;
		};
	}

	@Override
	default boolean matchField(int access, String name, String desc, String sig, Object value) {
		return false;
	}
}
