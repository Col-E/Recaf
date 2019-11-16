package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent classes.
 *
 * @author Matt
 */
public class ClassItem extends DirectoryItem {
	private final String name;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local item name.
	 * @param name
	 * 		Full class name.
	 */
	public ClassItem(JavaResource resource, String local, String name) {
		super(resource, local);
		this.name = name;
	}

	/**
	 * @return Contained class name.
	 */
	public String getClassName() {
		return name;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof ClassItem) {
			ClassItem c = (ClassItem) o;
			return name.compareTo(c.name);
		}
		return 1;
	}
}