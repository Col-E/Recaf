package me.coley.recaf.ui.control.tree.item;

/**
 * Item to wrap files of a {@link ResourceItem}.
 *
 * @author Matt Coley
 */
public class ResourceFilesItem extends BaseTreeItem {
	/**
	 * Create the item.
	 */
	public ResourceFilesItem() {
		init();
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, null, true);
	}
}
