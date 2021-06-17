package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.Recaf;
import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Tree item to contain class sub-items.
 *
 * @author Matt
 */
public class ClassFolderItem extends DirectoryItem {
	private final JavaResource resource = resource();

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public ClassFolderItem(JavaResource resource) {
		super(resource, "classes");
		// Add class sub-items in sorted order
		new TreeSet<>(resource.getClasses().keySet()).forEach(this::addClass);
	}

	protected void addClass(String name) {
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		// Prune tree directory middle section if it is obnoxiously long
		int maxDepth = Recaf.getController().config().display().maxTreeDepth;
		if (parts.size() > maxDepth) {
			String lastPart = parts.get(parts.size() - 1);
			// We keep only elements between
			// [0..maxDepth-1] and the last part
			parts = new ArrayList<>(parts.subList(0, maxDepth - 1));
			parts.add("...");
			parts.add(lastPart);
		}
		// Build directory structure
		StringBuilder sb = new StringBuilder();
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			sb.append(part).append('.');
			boolean isLeaf = parts.isEmpty();
			DirectoryItem child = item.getChild(part, isLeaf);
			if(child == null) {
				child = isLeaf ?
						new ClassItem(resource, part, name) :
						new PackageItem(resource, part, sb.substring(0, sb.length() - 1));
				item.addChild(part, child, isLeaf);
			}
			item = child;
		}
	}
}