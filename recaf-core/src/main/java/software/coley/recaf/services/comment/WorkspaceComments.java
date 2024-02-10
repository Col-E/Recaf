package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;

/**
 * Outline of a container for commented elements in a workspace.
 *
 * @author Matt Coley
 */
public interface WorkspaceComments extends Iterable<ClassComments> {
	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return Comments container for the class, creating a new container if none exist.
	 */
	@Nonnull
	ClassComments getOrCreateClassComments(@Nonnull ClassPathNode classPath);

	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return Comments container for the class, if comments exist for the class. Otherwise {@code null}.
	 */
	@Nullable
	ClassComments getClassComments(@Nonnull ClassPathNode classPath);
}
