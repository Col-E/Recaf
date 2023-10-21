package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.FieldMember;

import java.util.Collection;

/**
 * Predicate to use in {@link MemberFilteringVisitor} and {@link MemberRemovingVisitor}.
 * <br
 * Has a default always-false method matching implementation to facilitate SAM usage for fields.
 *
 * @author Matt Coley
 * @see MethodPredicate
 */
public interface FieldPredicate extends MemberPredicate {
	/**
	 * @param field
	 * 		Field to match.
	 *
	 * @return Predicate matching a single field.
	 */
	@Nonnull
	static FieldPredicate of(@Nonnull FieldMember field) {
		return (access, name, desc, sig, value) -> field.getName().equals(name) && field.getDescriptor().equals(desc);
	}

	/**
	 * @param fields
	 * 		Fields to match.
	 *
	 * @return Predicate matching a collection of fields.
	 */
	@Nonnull
	static FieldPredicate of(@Nonnull Collection<FieldMember> fields) {
		return (access, name, desc, sig, value) -> {
			for (FieldMember field : fields)
				if (field.getName().equals(name) && field.getDescriptor().equals(desc))
					return true;
			return false;
		};
	}

	@Override
	default boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
		return false;
	}
}
