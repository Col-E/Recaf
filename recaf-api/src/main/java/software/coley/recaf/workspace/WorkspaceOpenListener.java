package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.cdi.AutoRegisterWorkspaceListeners;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Listener for when new workspaces are opened.
 *
 * @author Matt Coley
 * @see AutoRegisterWorkspaceListeners Can be used to automatically register components implementing this type.
 */
public interface WorkspaceOpenListener {
	/**
	 * Called when {@link WorkspaceManager#setCurrent(Workspace)} passes.
	 *
	 * @param workspace
	 * 		New workspace assigned.
	 */
	void onWorkspaceOpened(@Nonnull Workspace workspace);
}