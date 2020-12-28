package me.coley.recaf.workspace;

import me.coley.recaf.workspace.resource.Resource;

/**
 * Listener for receiving workspace update events.
 *
 * @author Matt Coley
 */
public interface WorkspaceListener {
	/**
	 * @param workspace
	 * 		The workspace.
	 * @param library
	 * 		Library added to the workspace.
	 */
	void onAddLibrary(Workspace workspace, Resource library);

	/**
	 * @param workspace
	 * 		The workspace.
	 * @param library
	 * 		Library removed from the workspace.
	 */
	void onRemoveLibrary(Workspace workspace, Resource library);
}
