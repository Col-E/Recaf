package me.coley.recaf.ui.control.tree.item;

/**
 * Root element for {@link me.coley.recaf.ui.control.tree.WorkspaceTree}.
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
