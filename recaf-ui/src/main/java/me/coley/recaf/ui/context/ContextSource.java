package me.coley.recaf.ui.context;

import me.coley.recaf.ui.pane.WorkspacePane;

/**
 * The UI location where a context menu is created from.
 *
 * @author Matt Coley
 */
public enum ContextSource {
	/**
	 * Items shown in the {@link WorkspacePane}.
	 */
	WORKSPACE_TREE,
	/**
	 * TODO: Implement search view
	 */
	SEARCH_RESULTS,
	/**
	 * TODO: Implement code view (decompile)
	 */
	CODE_VIEW,
	/**
	 * TODO: Implement bytecode view (assembler)
	 */
	BYTECODE_VIEW;
}
