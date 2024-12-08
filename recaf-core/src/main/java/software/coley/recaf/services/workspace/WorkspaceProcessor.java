package software.coley.recaf.services.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Generic processor for use in {@link WorkspaceProcessingService}.
 *
 * @author Matt Coley
 * @see WorkspaceProcessingService Manages calling implementations of this type.
 */
public interface WorkspaceProcessor {
	/**
	 * Called when {@link WorkspaceManager#setCurrent(Workspace)} passes.
	 *
	 * @param workspace
	 * 		Workspace to process.
	 */
	void processWorkspace(@Nonnull Workspace workspace);

	/**
	 * @return Post processing task name.
	 */
	@Nonnull
	String name();
}
