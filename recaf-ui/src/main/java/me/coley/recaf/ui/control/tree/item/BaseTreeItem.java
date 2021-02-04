package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.ui.control.tree.WorkspaceTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Base tree item for {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public abstract class BaseTreeItem extends FilterableTreeItem<BaseTreeValue> implements Comparable<BaseTreeItem> {
	private final Map<String, BaseTreeItem> directoryChildren = new HashMap<>();
	private final Map<String, BaseTreeItem> fileChildren = new HashMap<>();

	/**
	 * This exists it needs to be called after the constructor completes for the implementation class.
	 */
	protected void init() {
		if (getValue() == null) {
			setValue(createTreeValue());
		}
	}

	/**
	 * Add the given child to the current item.
	 *
	 * @param item
	 * 		Child to add.
	 */
	public void addChild(BaseTreeItem item) {
		// Update child maps
		BaseTreeValue value = item.getValue();
		if (value.isDirectory()) {
			directoryChildren.put(value.getPathElementValue(), item);
		} else {
			fileChildren.put(value.getPathElementValue(), item);
		}
		// Add to tree
		addSourceChild(item);
	}

	/**
	 * Remove the given child from the current item.
	 *
	 * @param item
	 * 		Child to remove.
	 */
	public void removeChild(BaseTreeItem item) {
		// Update child maps
		BaseTreeValue value = item.getValue();
		if (value.isDirectory()) {
			directoryChildren.remove(value.getPathElementValue());
		} else {
			fileChildren.remove(value.getPathElementValue());
		}
		// Remove from tree
		removeSourceChild(item);
	}

	/**
	 * Get the named child leaf item.
	 *
	 * @param pathElementName
	 * 		Name of sub-item path.
	 *
	 * @return Tree item associated with the path.
	 */
	public BaseTreeItem getChildDirectory(String pathElementName) {
		return directoryChildren.get(pathElementName);
	}

	/**
	 * Get the named child branch item.
	 *
	 * @param pathElementName
	 * 		Name of sub-item path.
	 *
	 * @return Tree item associated with the path.
	 */
	public BaseTreeItem getChildFile(String pathElementName) {
		return fileChildren.get(pathElementName);
	}

	/**
	 * Creates the item to associate with the current tree item.
	 *
	 * @return Value to assign to item. Cannot be {@code null}.
	 */
	protected abstract BaseTreeValue createTreeValue();

	@Override
	public int compareTo(BaseTreeItem other) {
		boolean iDirectory = getValue().isDirectory();
		boolean oDirectory = other.getValue().isDirectory();
		// Ensure directories are first
		if (iDirectory && !oDirectory) {
			return -1;
		} else if (!iDirectory && oDirectory) {
			return 1;
		}
		// Sort based on name
		String iPath = getValue().getPathElementValue();
		String oPath = other.getValue().getPathElementValue();
		if (iPath == null || oPath == null) {
			return getClass().getName().compareTo(other.getClass().getName());
		}
		return iPath.compareTo(oPath);
	}
}
