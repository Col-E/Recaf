package me.coley.recaf.ui.control.tree;

/**
 * Passed to an {@link WorkspaceCellFactory} so that each {@link WorkspaceCell} can populate its context menu
 * with knowledge of where the cell is located in the UI. Some locations may want to offer different behavior.
 *
 * @author Matt Coley
 */
public enum WorkspaceTreeType {
	/**
	 * Tree belongs to {@link me.coley.recaf.ui.panel.WorkspacePanel}.
	 */
	WORKSPACE_NAVIGATION,
	/**
	 * TODO: Search results
	 */
	SEARCH_RESULTS
}
