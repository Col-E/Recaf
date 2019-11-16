package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent annotations.
 *
 * @author Matt
 */
public class AnnoItem extends DirectoryItem {
	private final String name;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local item name.
	 * @param name
	 * 		Full annotation name.
	 */
	public AnnoItem(JavaResource resource, String local, String name) {
		super(resource, local);
		this.name = name;
	}

	/**
	 * @return Contained class name.
	 */
	public String getAnnoName() {
		return name;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof AnnoItem) {
			AnnoItem c = (AnnoItem) o;
			return name.compareTo(c.name);
		}
		return 1;
	}
}