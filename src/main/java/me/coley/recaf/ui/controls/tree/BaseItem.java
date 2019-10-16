package me.coley.recaf.ui.controls.tree;

import javafx.scene.control.TreeItem;
import me.coley.recaf.workspace.JavaResource;

/**
 * Tree item base.
 *
 * @author Matt
 */
public class BaseItem extends TreeItem<JavaResource> {
	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public BaseItem(JavaResource resource) {
		setValue(resource);
	}

	/**
	 * For code clarity.
	 *
	 * @return JavaResource of the item.
	 */
	protected JavaResource resource() {
		return getValue();
	}
}