package me.coley.recaf.workspace;

import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource manager.
 *
 * @author Matt Coley
 */
public class Workspace {
	private final List<WorkspaceListener> listeners = new ArrayList<>();
	private final Resources resources;

	/**
	 * @param resources
	 * 		Resources for the workspace.
	 */
	public Workspace(Resources resources) {
		this.resources = resources;
	}

	/**
	 * Called when the workspace is removed from the controller.
	 */
	public void cleanup() {
		// Remove listeners
		listeners.clear();
		resources.getPrimary().clearListeners();
		resources.getLibraries().forEach(Resource::clearListeners);
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
			listeners.forEach(listener -> listener.onAddLibrary(this, library));
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
			listeners.forEach(listener -> listener.onRemoveLibrary(this, library));
			library.clearListeners();
		}
	}

	/**
	 * @return Workspace resources.
	 */
	public Resources getResources() {
		return resources;
	}

	/**
	 * @param listener
	 * 		New workspace event listener to add.
	 */
	public void addListener(WorkspaceListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Workspace event listener to remove.
	 */
	public void removeListener(WorkspaceListener listener) {
		listeners.remove(listener);
	}
}
