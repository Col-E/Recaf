package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.Recaf;
import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Tree item to contain class sub-items.
 *
 * @author Matt
 */
public class FileFolderItem extends DirectoryItem {
	private final JavaResource resource = resource();

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public FileFolderItem(JavaResource resource) {
		super(resource, "files");
		// Add class sub-items in sorted order
		new TreeSet<>(resource.getFiles().keySet()).forEach(this::addFile);
	}

	protected void addFile(String name) {
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		// Prune tree directory middle section if it is obnoxiously long
		int maxDepth = Recaf.getController().config().display().maxTreeDepth;
		if (parts.size() > maxDepth) {
			while (parts.size() > maxDepth) {
				parts.remove(maxDepth - 1);
			}
			parts.add(maxDepth - 1, "...");
		}
		// Build directory structure
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			DirectoryItem child = item.getChild(part, isLeaf);
			if(child == null) {
				child = isLeaf ?
						new FileItem(resource, part, name) :
						new DirectoryItem(resource, part);
				item.addChild(part, child, isLeaf);
			}
			item = child;
		}
	}
}