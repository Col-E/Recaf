package me.coley.recaf.ui.control.tree;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;

/**
 * Tree view implementation for {@link BaseTreeValue} types.
 *
 * @author Matt Coley
 */
public class WorkspaceTree extends TreeView<BaseTreeValue> {
	/**
	 * @param treeType
	 * 		Tree type. Passed to {@link WorkspaceCellFactory}.
	 */
	public WorkspaceTree(CellOriginType treeType) {
		setCellFactory(new WorkspaceCellFactory(treeType));
		NodeEvents.addKeyPressHandler(this, this::onKeyPressed);
	}

	private void onKeyPressed(KeyEvent e) {
		TreeItem<?> item = getSelectionModel().getSelectedItem();
		if (e.getCode() == KeyCode.ENTER) {
			// Open selected
			if (item.isLeaf()) {
				openItem(item);
			}
			// Recursively open children until multiple options are present
			else {
				BaseTreeItem.recurseOpen(item);
			}
		}
	}

	/**
	 * Open the content of the tree item in the UI.
	 *
	 * @param item
	 * 		Item representing value.
	 */
	public static void openItem(TreeItem<?> item) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null)
			return;
		Resources resources = workspace.getResources();
		if (item instanceof ClassItem) {
			ClassItem ci = (ClassItem) item;
			String name = ci.getClassName();
			CommonUX.openClass(resources.getClass(name));
		} else if (item instanceof DexClassItem) {
			DexClassItem dci = (DexClassItem) item;
			String name = dci.getClassName();
			CommonUX.openClass(resources.getDexClass(name));
		} else if (item instanceof FileItem) {
			FileItem ri = (FileItem) item;
			String name = ri.getFileName();
			CommonUX.openFile(resources.getFile(name));
		}
	}
}
