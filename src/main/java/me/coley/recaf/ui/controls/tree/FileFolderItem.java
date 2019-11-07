package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Tree item to contain class sub-items.
 *
 * @author Matt
 */
public class FileFolderItem extends DirectoryItem {
	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public FileFolderItem(JavaResource resource) {
		super(resource, null);
		// Add class sub-items
		new TreeSet<>(resource.getFiles().keySet()).forEach(this::addFile);
	}

	protected void addFile(String name) {
		JavaResource resource = resource();
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			DirectoryItem child = item.getChild(part, parts.isEmpty());
			if(child == null) {
				child = parts.isEmpty() ?
						new FileItem(resource, part, name) :
						new DirectoryItem(resource, part);
				item.addChild(part, child);
			}
			item = child;
		}
	}
}