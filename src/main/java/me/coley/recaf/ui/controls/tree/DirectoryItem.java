package me.coley.recaf.ui.controls.tree;

import javafx.scene.control.TreeItem;
import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Item to be used
 *
 * @author Matt
 */
public class DirectoryItem extends BaseItem implements Comparable<DirectoryItem> {
	// Differentiate directories and leaves to account for overlapping names.
	private final Map<String, DirectoryItem> localToDir = new HashMap<>();
	private final Map<String, DirectoryItem> localToLeaf = new HashMap<>();
	private final String local;


	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Partial name of item.
	 */
	public DirectoryItem(JavaResource resource, String local) {
		super(resource);
		this.local = local;
	}

	@Override
	public void removeSourceChild(TreeItem<JavaResource> child) {
		boolean isClass = child instanceof ClassItem;
		boolean isDir = child instanceof PackageItem;
		if (isClass || isDir) {
			String childLocal = ((DirectoryItem) child).local;
			if (isClass)
				localToLeaf.remove(childLocal);
			else
				localToDir.remove(childLocal);
		}
		super.removeSourceChild(child);
	}


	/**
	 * Used for display.
	 *
	 * @return Get local item name.
	 */
	public String getLocalName() {
		return local;
	}

	/**
	 * Add a child by the local name.
	 *
	 * @param local
	 * 		Local name of child.
	 * @param child
	 * 		Child to add.
	 * @param isLeaf
	 * 		Indicator that the added child is the final element.
	 */
	public void addChild(String local, DirectoryItem child, boolean isLeaf) {
		if (isLeaf)
			localToLeaf.put(local, child);
		else
			localToDir.put(local, child);
		addSourceChild(child);
	}

	/**
	 * @param local
	 * 		Local name of child.
	 * @param isLeaf
	 * 		Does the local name belong to a leaf.
	 *
	 * @return Child item by local name.
	 */
	public DirectoryItem getChild(String local, boolean isLeaf) {
		if (isLeaf)
			return localToLeaf.get(local);
		return localToDir.get(local);
	}

	/**
	 * A path is specified as multiple local names joined by '/'.
	 *
	 * @param path
	 * 		Path to child.
	 *
	 * @return Child item by path.
	 */
	public DirectoryItem getDeepChild(String path) {
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(path.split("/")));
		while(!parts.isEmpty() && item != null) {
			String part = parts.remove(0);
			item = item.getChild(part, parts.isEmpty());
		}
		return item;
	}

	/**
	 * Expand all parents to this item.
	 */
	public void expandParents() {
		TreeItem<?> item = this;
		while ((item = item.getParent()) != null)
			item.setExpanded(true);
	}

	@Override
	public int compareTo(DirectoryItem o) {
		// Ensure classes do not appear above adjacent packages
		if (this instanceof ClassItem && !(o instanceof ClassItem))
			return 1;
		else if (o instanceof ClassItem && !(this instanceof ClassItem))
			return -1;
		// Compare local name
		return local.compareTo(o.local);
	}
}