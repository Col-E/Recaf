package me.coley.recaf.ui.prompt;

import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.List;

/**
 * Drop result container.
 *
 * @author Matt Coley
 */
public class WorkspaceAction {
	private final WorkspaceActionType action;
	private final Workspace workspace;
	private final List<Resource> libraries;

	WorkspaceAction(WorkspaceActionType action, Workspace workspace, List<Resource> libraries) {
		this.action = action;
		this.workspace = workspace;
		this.libraries = libraries;
	}

	/**
	 * @return Loaded library resources.
	 * Will be {@code null} if {@link #action} is not {@link WorkspaceActionType#ADD_TO_WORKSPACE}
	 */
	public List<Resource> getLibraries() {
		return libraries;
	}

	/**
	 * @return Loaded workspace.
	 * Will be {@code null} if {@link #action} is not {@link WorkspaceActionType#CREATE_NEW_WORKSPACE}
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * @return Action of the result.
	 */
	public WorkspaceActionType getAction() {
		return action;
	}
}
