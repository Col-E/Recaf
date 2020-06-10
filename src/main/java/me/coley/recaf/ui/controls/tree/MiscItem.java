package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent any other potential data.
 *
 * @author Matt
 */
public class MiscItem extends DirectoryItem {
	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local item name.
	 */
	public MiscItem(JavaResource resource, String local) {
		super(resource, local);
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof MiscItem) {
			MiscItem c = (MiscItem) o;
			return super.compareTo(o);
		}
		return 1;
	}
}