package me.coley.recaf.ui.behavior;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;

/**
 * Children of this type represent a class, and thus should offer class oriented UX capabilities.
 *
 * @author Matt Coley
 */
public interface ClassRepresentation extends Representation, Updatable<CommonClassInfo> {
	/**
	 * @return Current class item being represented.
	 */
	CommonClassInfo getCurrentClassInfo();

	/**
	 * @return {@code true} if the current representation of a class supports
	 * {@link #selectMember(MemberInfo) selection of members}.
	 */
	boolean supportsMemberSelection();

	/**
	 * @return {@code true} when {@link #selectMember(MemberInfo)} is ready to be called.
	 * Member selection may be supported but not ready to be done.
	 */
	boolean isMemberSelectionReady();

	/**
	 * @param memberInfo
	 * 		Member to select in the current class.
	 */
	void selectMember(MemberInfo memberInfo);
}
