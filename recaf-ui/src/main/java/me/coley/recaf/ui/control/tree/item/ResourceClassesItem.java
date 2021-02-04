package me.coley.recaf.ui.control.tree.item;

/**
 * Item to wrap classes of a {@link ResourceItem}.
 *
 * @author Matt Coley
 */
public class ResourceClassesItem extends BaseTreeItem {
	/**
	 * Create the item.
	 */
	public ResourceClassesItem() {
		init();
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, null, true);
	}
}
