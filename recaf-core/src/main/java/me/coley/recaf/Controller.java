package me.coley.recaf;

import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.workspace.Workspace;

/**
 * Backbone of all internal Recaf handling.
 *
 * @author Matt Coley
 */
public class Controller {
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
		this.workspace = workspace;
		this.services.updateWorkspace(workspace);
		// TODO: Create listener and call "onWorkspaceOpen" or something here?
		//  - Will want to consider design of "singluar" listener vs "multiple"
		//  - May want more than one listener on an object.
		//  - This is also relevant for resource's class/file maps
		// Open new workspace, if non-null
		if (workspace != null) {
			presentation.workspaceLayer().openWorkspace(workspace);
		}
	}
}
