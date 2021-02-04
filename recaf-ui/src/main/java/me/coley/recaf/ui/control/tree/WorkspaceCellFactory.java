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

	@Override
	public TreeCell<BaseTreeValue> call(TreeView<BaseTreeValue> param) {
		return new WorkspaceCell();
	}
}
