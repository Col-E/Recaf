package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.ClassMember;

import java.util.Collection;

/**
 * Predicate to use in {@link MemberFilteringVisitor} and {@link MemberRemovingVisitor}.
 *
 * @author Matt Coley
 * @see FieldPredicate
 * @see MethodPredicate
 */
public interface MemberPredicate {
	/**
	 * @param member
	 * 		Field or method to match.
	 *
	 * @return Predicate matching a single member.
	 */
	@Nonnull
	static MemberPredicate of(@Nonnull ClassMember member) {
		return new MemberPredicate() {
			@Override
			public boolean matchField(int access, String name, String desc, String sig, Object value) {
				if (member.isField())
					return name.equals(member.getName()) && desc.equals(member.getDescriptor());
				return false;
			}

			@Override
			public boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
				if (member.isMethod())
					return name.equals(member.getName()) && desc.equals(member.getDescriptor());
				return false;
			}
		};
	}

	/**
	 * @param members
	 * 		Fields and methods to match.
	 *
	 * @return Predicate matching a collection of fields and methods.
	 */
	@Nonnull
	static MemberPredicate of(@Nonnull Collection<ClassMember> members) {
		return new MemberPredicate() {
			@Override
			public boolean matchField(int access, String name, String desc, String sig, Object value) {
				for (ClassMember member : members)
					if (member.isField())
						return name.equals(member.getName()) && desc.equals(member.getDescriptor());
				return false;
			}

			@Override
			public boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
				for (ClassMember member : members)
					if (member.isMethod())
						return name.equals(member.getName()) && desc.equals(member.getDescriptor());
				return false;
			}
		};
	}

	/**
	 * @param access
	 * 		Field access flags.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param sig
	 * 		Field generic signature.
	 * @param value
	 * 		Field value.
	 *
	 * @return Match result.
	 */
	boolean matchField(int access, String name, String desc, String sig, Object value);

	/**
	 * @param access
	 * 		Method access flags.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param sig
	 * 		Method generic signature.
	 * @param exceptions
	 * 		Method exceptions.
	 *
	 * @return Match result.
	 */
	boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions);
}
