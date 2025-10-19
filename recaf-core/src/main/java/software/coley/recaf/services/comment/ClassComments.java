package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

import java.time.Instant;

/**
 * Outline of a container for comments for a class and its contents.
 *
 * @author Matt Coley
 */
public interface ClassComments {
	/**
	 * @return Time of the first created comment.
	 */
	@Nonnull
	Instant getCreationTime();

	/**
	 * @return Time of the last comment update.
	 */
	@Nonnull
	Instant getLastUpdatedTime();

	/**
	 * @return {@code true} when any comments exist on this class, or any of its declared members.
	 */
	boolean hasComments();

	/**
	 * @return Class comment, if any.
	 */
	@Nullable
	String getClassComment();

	/**
	 * @param comment
	 * 		New class comment, or {@code null} to remove an existing comment.
	 */
	void setClassComment(@Nullable String comment);

	/**
	 * @param member
	 * 		Field to look up.
	 *
	 * @return Field comment, if any.
	 */
	@Nullable
	default String getFieldComment(@Nonnull FieldMember member) {
		return getFieldComment(member.getName(), member.getDescriptor());
	}

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 *
	 * @return Field comment, if any.
	 */
	@Nullable
	String getFieldComment(@Nonnull String name, @Nonnull String descriptor);

	/**
	 * @param member
	 * 		Method to look up.
	 *
	 * @return Method comment, if any.
	 */
	@Nullable
	default String getMethodComment(@Nonnull MethodMember member) {
		return getMethodComment(member.getName(), member.getDescriptor());
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Method comment, if any.
	 */
	@Nullable
	String getMethodComment(@Nonnull String name, @Nonnull String descriptor);

	/**
	 * @param member
	 * 		Field to assign comment to.
	 * @param comment
	 * 		New field comment, or {@code null} to remove an existing comment.
	 */
	default void setFieldComment(@Nonnull FieldMember member, @Nullable String comment) {
		setFieldComment(member.getName(), member.getDescriptor(), comment);
	}

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param comment
	 * 		New field comment, or {@code null} to remove an existing comment.
	 */
	void setFieldComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment);

	/**
	 * @param member
	 * 		Method to assign comment to.
	 * @param comment
	 * 		New method comment, or {@code null} to remove an existing comment.
	 */
	default void setMethodComment(@Nonnull MethodMember member, @Nullable String comment) {
		setMethodComment(member.getName(), member.getDescriptor(), comment);
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param comment
	 * 		New method comment, or {@code null} to remove an existing comment.
	 */
	void setMethodComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment);
}
