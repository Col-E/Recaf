package me.coley.recaf.ui.control.tree;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import me.coley.recaf.ui.control.tree.item.BaseTreeValue;

/**
 * Cell factory for {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public class WorkspaceCellFactory implements
		Callback<TreeView<BaseTreeValue>, TreeCell<BaseTreeValue>> {
	private final CellOriginType type;

	/**
	 * @param type
	 * 		Tree type, allowing cells to be more specific with their context menus.
	 */
	public WorkspaceCellFactory(CellOriginType type) {
		this.type = type;
	}

	@Override
	public TreeCell<BaseTreeValue> call(TreeView<BaseTreeValue> param) {
		return new WorkspaceCell(type);
	}
}
