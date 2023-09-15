package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Subtype of {@link WorkspaceOpenListener} for use in {@link WorkspaceProcessing}.
 *
 * @author Matt Coley
 * @see WorkspaceProcessing Manages calling implementations of this type.
 */
public interface WorkspaceProcessor {
	/**
	 * Called when {@link WorkspaceManager#setCurrent(Workspace)} passes.
	 *
	 * @param workspace
	 * 		New workspace assigned.
	 */
	void onWorkspaceOpened(@Nonnull Workspace workspace);

	/**
	 * @return Post processing task name.
	 */
	@Nonnull
	String name();
}
