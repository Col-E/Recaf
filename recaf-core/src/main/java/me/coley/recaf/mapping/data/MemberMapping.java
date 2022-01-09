package me.coley.recaf.mapping.data;

public interface MemberMapping {
	/**
	 * @return Name of class defining the member.
	 */
	String getOwnerName();

	/**
	 * @return Descriptor type of the member.   May be {@code null} for fields.
	 */
	String getDesc();

	/**
	 * @return Pre-mapping member name.
	 */
	String getOldName();

	/**
	 * @return Post-mapping member name.
	 */
	String getNewName();

	/**
	 * @return {@code true} when the member is a field.
	 * {@code false} for methods.
	 */
	boolean isField();
}
