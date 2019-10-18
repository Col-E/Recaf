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
	 */
	public void addChild(String local, DirectoryItem child) {
		if (child instanceof ClassItem || child instanceof ResourceItem)
			localToLeaf.put(local, child);
		else
			localToDir.put(local, child);
		getSourceChildren().add(child);
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
		return local.compareTo(o.local);
	}
}