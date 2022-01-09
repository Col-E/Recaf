package me.coley.recaf.ui.behavior;

import me.coley.recaf.code.MemberInfo;

/**
 * Children of this type represent a {@link #getTargetMember() member of a class} and thus
 * should offer member oriented UX capabilities.
 *
 * @author Matt Coley
 */
public interface MemberEditor extends ClassRepresentation {
	/**
	 * @return The member being edited.
	 */
	MemberInfo getTargetMember();

	/**
	 * @param targetMember
	 * 		New member value to edit.
	 */
	void setTargetMember(MemberInfo targetMember);
}
