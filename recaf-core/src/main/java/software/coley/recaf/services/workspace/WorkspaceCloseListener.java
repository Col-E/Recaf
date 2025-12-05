package software.coley.recaf.services.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Listener for when old workspaces are closed.
 *
 * @author Matt Coley
 */
public interface WorkspaceCloseListener extends PrioritySortable {
	/**
	 * Called when {@link WorkspaceManager#setCurrent(Workspace)} passes and a prior workspace is removed.
	 *
	 * @param workspace
	 * 		New workspace assigned.
	 */
	void onWorkspaceClosed(@Nonnull Workspace workspace);
}
