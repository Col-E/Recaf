package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param comment
	 * 		New field comment, or {@code null} to remove an existing comment.
	 */
	void setFieldComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment);

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
