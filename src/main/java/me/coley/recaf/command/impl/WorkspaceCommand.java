package me.coley.recaf.command.impl;

import me.coley.recaf.workspace.Workspace;

/**
 * Base for commands that depend on an active workspace.
 *
 * @author Matt
 */
public class WorkspaceCommand {
	protected Workspace workspace;

	/**
	 * Set the command's workspace.
	 *
	 * @param workspace
	 * 		Workspace to operate on.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Ensures the command can run, throwing an exception otherwise.
	 */
	public void verify() {
		if (workspace == null)
			throw new IllegalStateException("Command required workspace to run, but was not given any!");
	}
}
