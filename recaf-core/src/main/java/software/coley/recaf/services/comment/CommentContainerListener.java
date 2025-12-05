package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.path.ClassPathNode;

/**
 * Listener for receiving updates when comment containers are removed.
 *
 * @author Matt Coley
 */
public interface CommentContainerListener extends PrioritySortable {
	/**
	 * @param path
	 * 		Path to class.
	 * @param comments
	 * 		Newly made comment container for the class.
	 */
	default void onClassContainerCreated(@Nonnull ClassPathNode path, @Nullable ClassComments comments) {}

	/**
	 * @param path
	 * 		Path to class.
	 * @param comments
	 * 		Removed comment container of the class.
	 */
	default void onClassContainerRemoved(@Nonnull ClassPathNode path, @Nullable ClassComments comments) {}
}
