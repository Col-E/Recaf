package me.coley.recaf.ui.control.tree.item;

/**
 * Root element for {@link me.coley.recaf.ui.panel.ResultsPane}.
 *
 * @author Matt Coley
 */
public class ResultsRootValue extends BaseTreeValue {
	/**
	 * @param owner
	 * 		Associated item.
	 */
	public ResultsRootValue(ResultsRootItem owner) {
		super(owner, null, true);
	}
}
