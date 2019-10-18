package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Tree item base.
 *
 * @author Matt
 */
public class BaseItem extends FilterableTreeItem<JavaResource> {
	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public BaseItem(JavaResource resource) {
		super(resource);
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