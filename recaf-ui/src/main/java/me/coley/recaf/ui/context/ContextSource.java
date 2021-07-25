package me.coley.recaf.ui.context;

/**
 * The UI location where a context menu is created from.
 *
 * @author Matt Coley
 */
public enum ContextSource {
	/**
	 * Items shown in the {@link me.coley.recaf.ui.panel.WorkspacePanel}.
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
