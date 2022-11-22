package me.coley.recaf.workspace;

/**
 * Listener for receiving workspace assignment events in the {@link me.coley.recaf.Controller}
 *
 * @author Matt Coley
 */
public interface WorkspaceOpenListener {
	/**
	 * @param workspace
	 * 		Newly opened workspace.
	 */
	void onWorkspaceOpened(Workspace workspace);
}
