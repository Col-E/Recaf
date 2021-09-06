package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;

/**
 * Root element for {@link WorkspaceTreeWrapper}.
 *
 * @author Matt Coley
 */
public class WorkspaceRootValue extends BaseTreeValue {
	/**
	 * @param owner
	 * 		Associated item.
	 */
	public WorkspaceRootValue(WorkspaceRootItem owner) {
		super(owner, null, true);
	}
}
