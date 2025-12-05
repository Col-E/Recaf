package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;

/**
 * Listener for receiving updates when comments are added to classes, fields, and methods.
 *
 * @author Matt Coley
 */
public interface CommentUpdateListener extends PrioritySortable {
	/**
	 * @param path
	 * 		Path to class commented.
	 * @param comment
	 * 		Content of comment for the class. Can be {@code null} to denote removal of a comment.
	 */
	default void onClassCommentUpdated(@Nonnull ClassPathNode path, @Nullable String comment) {}

	/**
	 * @param path
	 * 		Path to field commented.
	 * @param comment
	 * 		Content of comment for the field. Can be {@code null} to denote removal of a comment.
	 */
	default void onFieldCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {}

	/**
	 * @param path
	 * 		Path to method commented.
	 * @param comment
	 * 		Content of comment for the method. Can be {@code null} to denote removal of a comment.
	 */
	default void onMethodCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {}
}
