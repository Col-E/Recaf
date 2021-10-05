package me.coley.recaf.ui.prompt;

import me.coley.recaf.workspace.resource.Resource;

import java.util.List;

/**
 * Type of action to run as a result of a file action.
 *
 * @author Matt Coley
 */
public enum WorkspaceActionType {
	ADD_TO_WORKSPACE,
	CREATE_NEW_WORKSPACE,
	CANCEL;

	/**
	 * @param resources
	 * 		Resources to operate on.
	 *
	 * @return Action to take based on the current action type.
	 */
	public WorkspaceAction createResult(List<Resource> resources) {
		switch (this) {
			case CREATE_NEW_WORKSPACE:
				return WorkspaceDropPrompts.workspace(WorkspaceDropPrompts.createWorkspace(resources));
			case ADD_TO_WORKSPACE:
				return WorkspaceDropPrompts.add(resources);
			case CANCEL:
			default:
				return WorkspaceDropPrompts.cancel();
		}
	}
}
