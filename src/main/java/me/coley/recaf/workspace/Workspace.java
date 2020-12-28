package me.coley.recaf.workspace;

import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;

/**
 * Resource manager.
 *
 * @author Matt Coley
 */
public class Workspace {
	private final Resources resources;
	private WorkspaceListener listener;

	/**
	 * @param resources
	 * 		Resources for the workspace.
	 */
	public Workspace(Resources resources) {
		this.resources = resources;
	}

	/**
	 * Add a library to the workspace.
	 *
	 * @param library
	 * 		Library to add.
	 */
	public void addLibrary(Resource library) {
		if (!resources.getLibraries().contains(library)) {
			resources.getLibraries().add(library);
			listener.onAddLibrary(this, library);
		}
	}

	/**
	 * Remove a library from the workspace.
	 *
	 * @param library
	 * 		Library to remove.
	 */
	public void removeLibrary(Resource library) {
		if (resources.getLibraries().remove(library)) {
			listener.onRemoveLibrary(this, library);
		}
	}

	/**
	 * @return Workspace resources.
	 */
	public Resources getResources() {
		return resources;
	}

	/**
	 * @return Workspace event listener.
	 */
	public WorkspaceListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 * 		New workspace event listener.
	 */
	public void setListener(WorkspaceListener listener) {
		this.listener = listener;
	}
}
