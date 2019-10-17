package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Item to be used
 *
 * @author Matt
 */
public class DirectoryItem extends BaseItem implements Comparable<DirectoryItem> {
	// TODO: Handle cases like: "a/a/a.class" vs "a/a.class"
	// - Use information about full paths to choose from directory vs leaf nodes
	private final Map<String, DirectoryItem> localToChild = new HashMap<>();
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
		localToChild.put(local, child);
		// Insert in ordered position
		int index = Arrays.binarySearch(getChildren().toArray(), child);
		if(index < 0)
			index = -(index + 1);
		getChildren().add(index, child);
	}

	/**
	 * @param local
	 * 		Local name of child.
	 *
	 * @return Child item by local name.
	 */
	public DirectoryItem getChild(String local) {
		return localToChild.get(local);
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
			item = item.getChild(part);
		}
		return item;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		return local.compareTo(o.local);
	}
}