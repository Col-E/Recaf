package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.ClassMember;

/**
 * Member predicate configured to match a single {@link ClassMember}.
 *
 * @author Matt Coley
 */
public class SingleMemberPredicate implements MemberPredicate {
	private final ClassMember member;

	/**
	 * @param member
	 * 		Member to match.
	 */
	public SingleMemberPredicate(@Nonnull ClassMember member) {
		this.member = member;
	}

	@Override
	public boolean matchField(int access, String name, String desc, String sig, Object value) {
		if (member.isField())
			return name.equals(member.getName()) && desc.equals(member.getName());
		return false;
	}

	@Override
	public boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (member.isMethod())
			return name.equals(member.getName()) && desc.equals(member.getName());
		return false;
	}
}
