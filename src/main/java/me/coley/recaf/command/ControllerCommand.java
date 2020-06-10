package me.coley.recaf.command;

import me.coley.recaf.control.Controller;
import me.coley.recaf.workspace.Workspace;

/**
 * Base for commands that depend on the current workspace in a controller.
 *
 * @author Matt
 */
public abstract class ControllerCommand {
	private Controller controller;

	/**
	 * Set the command's controller.
	 *
	 * @param controller
	 * 		Controller with the workspace to operate on.
	 */
	public void setController(Controller controller) {
		this.controller = controller;
	}

	/**
	 * Ensures the command can run, throwing an exception otherwise.
	 */
	public void verify() {
		if (controller == null || controller.getWorkspace() == null)
			throw new IllegalStateException("Command required controller/workspace to run, but was not given any!");
	}

	/**
	 * @return Controller with the workspace to operate on.
	 */
	protected Controller getController() {
		return controller;
	}

	/**
	 * @return Workspace to operate on.
	 */
	protected Workspace getWorkspace() {
		return getController().getWorkspace();
	}
}
