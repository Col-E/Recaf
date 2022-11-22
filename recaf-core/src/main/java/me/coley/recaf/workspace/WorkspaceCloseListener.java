package me.coley.recaf.workspace;

/**
 * Listener for receiving workspace closure events in the {@link me.coley.recaf.Controller}
 *
 * @author Matt Coley
 */
public interface WorkspaceCloseListener {
	/**
	 * @param workspace
	 * 		Closed workspace.
	 */
	void onWorkspaceClosed(Workspace workspace);
}
