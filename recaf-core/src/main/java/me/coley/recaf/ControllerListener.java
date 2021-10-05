package me.coley.recaf;

import me.coley.recaf.workspace.Workspace;

/**
 * Listener for receiving controller update events.
 *
 * @author Matt Coley
 */
public interface ControllerListener {
	/**
	 * Called when a new workspace is loaded.
	 *
	 * @param oldWorkspace
	 * 		Prior workspace. May be {@code null}.
	 * @param newWorkspace
	 * 		New workspace. May be {@code null}.
	 */
	void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace);
}
