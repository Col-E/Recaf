package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.cdi.AutoRegisterWorkspaceListeners;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Listener for when old workspaces are closed.
 *
 * @author Matt Coley
 * @see AutoRegisterWorkspaceListeners Can be used to automatically register components implementing this type.
 */
public interface WorkspaceCloseListener {
	/**
	 * Called when {@link WorkspaceManager#setCurrent(Workspace)} passes and a prior workspace is removed.
	 *
	 * @param workspace
	 * 		New workspace assigned.
	 */
	void onWorkspaceClosed(@Nonnull Workspace workspace);
}
