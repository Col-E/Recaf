package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent files.
 *
 * @author Matt
 */
public class FileItem extends DirectoryItem {
	private final String name;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local file name.
	 * @param name
	 * 		Full file name.
	 */
	public FileItem(JavaResource resource, String local, String name) {
		super(resource, local);
		this.name = name;
	}

	/**
	 * @return Contained file name.
	 */
	public String getFileName() {
		return name;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof FileItem) {
			FileItem c = (FileItem) o;
			return name.compareTo(c.name);
		}
		return 1;
	}
}