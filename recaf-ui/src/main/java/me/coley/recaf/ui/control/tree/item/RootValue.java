package me.coley.recaf.ui.control.tree.item;

/**
 * Root element for {@link me.coley.recaf.ui.control.tree.WorkspaceTree}.
 *
 * @author Matt Coley
 */
public class RootValue extends BaseTreeValue {
	/**
	 * @param owner
	 * 		Associated item.
	 */
	public RootValue(RootItem owner) {
		super(owner, null, true);
	}
}
