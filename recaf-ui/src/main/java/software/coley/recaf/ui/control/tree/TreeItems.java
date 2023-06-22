package software.coley.recaf.ui.control.tree;

import javafx.scene.control.TreeItem;

/**
 * Utilities for {@link TreeItem} types.
 *
 * @author Matt Coley
 */
public class TreeItems {
	/**
	 * Expand all parents to this item.
	 */
	public static void expandParents(TreeItem<?> item) {
		while ((item = item.getParent()) != null)
			item.setExpanded(true);
	}

	/**
	 * Opens children recursively as long as only as there is only a path of single children.
	 *
	 * @param item
	 * 		Item to recursively open.
	 */
	public static void recurseOpen(TreeItem<?> item) {
		item.setExpanded(true);
		if (item.getChildren().size() == 1)
			recurseOpen(item.getChildren().get(0));
	}
}
