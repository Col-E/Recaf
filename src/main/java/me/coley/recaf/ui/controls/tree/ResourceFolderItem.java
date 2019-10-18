package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Tree item to contain class sub-items.
 *
 * @author Matt
 */
public class ResourceFolderItem extends DirectoryItem {
	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public ResourceFolderItem(JavaResource resource) {
		super(resource, null);
		// Add class sub-items
		new TreeSet<>(resource.getResources().keySet()).forEach(this::addResource);
	}

	protected void addResource(String name) {
		JavaResource resource = resource();
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			DirectoryItem child = item.getChild(part, parts.isEmpty());
			if(child == null) {
				child = parts.isEmpty() ?
						new ResourceItem(resource, part, name) :
						new DirectoryItem(resource, part);
				item.addChild(part, child);
			}
			item = child;
		}
	}
}