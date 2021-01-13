package me.coley.recaf;

import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.workspace.Workspace;

import java.util.HashSet;
import java.util.Set;

/**
 * Backbone of all internal Recaf handling.
 *
 * @author Matt Coley
 */
public class Controller {
	private final Set<ControllerListener> listeners = new HashSet<>();
	private final Services services = new Services(this);
	private final Presentation presentation;
	private Workspace workspace;

	/**
	 * @param presentation
	 * 		Presentation implementation.
	 */
	public Controller(Presentation presentation) {
		this.presentation = presentation;
	}

	/**
	 * @return Controller services.
	 */
	public Services getServices() {
		return services;
	}

	/**
	 * @return Presentation implementation.
	 */
	public Presentation getPresentation() {
		return presentation;
	}

	/**
	 * @return Current open workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Add a new controller listener.
	 *
	 * @param listener
	 * 		New controller listener.
	 */
	public void addListener(ControllerListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a controller listener.
	 *
	 * @param listener
	 * 		Controller listener to remove.
	 */
	public void removeListener(ControllerListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Set the current workspace. Will close an existing one if it exists.
	 *
	 * @param workspace
	 * 		New workspace, or {@code null} to close the workspace without a replacement.
	 */
	public void setWorkspace(Workspace workspace) {
		// Close current workspace
		if (this.workspace != null) {
			presentation.workspaceLayer().closeWorkspace(this.workspace);
			this.workspace.cleanup();
		}
		// Set new workspace & update services
		Workspace oldWorkspace = this.workspace;
		this.workspace = workspace;
		this.services.updateWorkspace(workspace);
		listeners.forEach(l -> l.onNewWorkspace(oldWorkspace, workspace));
		// Open new workspace, if non-null
		if (workspace != null) {
			presentation.workspaceLayer().openWorkspace(workspace);
		}
	}
}
