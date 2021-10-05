package me.coley.recaf.ui.control.tree;

import me.coley.recaf.ui.pane.WorkspacePane;

/**
 * Passed to an {@link me.coley.recaf.ui.util.CellFactory} so that each {@link javafx.scene.control.Cell}
 * can populate its context menu with knowledge of where the cell is located in the UI.
 * Some locations may want to offer different behavior.
 *
 * @author Matt Coley
 */
public enum CellOriginType {
	/**
	 * Tree belongs to {@link WorkspacePane}.
	 */
	WORKSPACE_NAVIGATION,
	/**
	 * List belongs to {@link me.coley.recaf.ui.prompt.QuickNavPrompt}.
	 */
	QUICK_NAV,
	/**
	 * Tree belongs to {@link me.coley.recaf.ui.pane.SearchPane}.
	 */
	SEARCH_RESULTS
}
