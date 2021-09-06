package me.coley.recaf.ui.control.tree.item;

import javafx.scene.control.TreeItem;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.tree.WorkspaceTree;
import me.coley.recaf.workspace.resource.Resource;

import java.util.*;
import java.util.function.Function;

/**
 * Base tree item for {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public abstract class BaseTreeItem extends FilterableTreeItem<BaseTreeValue> implements Comparable<BaseTreeItem> {
	protected static final String FLATTENED_ITEM = "...";
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
		if (value.getItemType().isDirectory()) {
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
		if (value.getItemType().isDirectory()) {
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
	 * Expand all parents to this item.
	 */
	public void expandParents() {
		TreeItem<?> item = this;
		while ((item = item.getParent()) != null)
			item.setExpanded(true);
	}

	/**
	 * Opens children recursively as long as only as there is only a path of single children.
	 *
	 * @param item
	 * 		Item to recursively open.
	 */
	public static void recurseOpen(TreeItem<?> item) {
		item.setExpanded(true);
		if (item.getChildren().size() == 1)
			recurseOpen(item.getChildren().get(0));
	}

	/**
	 * @return Resource that contains the data the current item represents.
	 */
	public Resource getContainingResource() {
		if (this instanceof ResourceItem) {
			return ((ResourceItem) this).getResource();
		}
		if (getParent() != null) {
			BaseTreeItem parent = (BaseTreeItem) getParent();
			return parent.getContainingResource();
		}
		return null;
	}

	/**
	 * Creates the item to associate with the current tree item.
	 *
	 * @return Value to assign to item. Cannot be {@code null}.
	 */
	protected abstract BaseTreeValue createTreeValue();

	@Override
	protected void onMatchResult(TreeItem<BaseTreeValue> child, boolean matched) {
		// Expand items that match, hide those that do not.
		if (matched && child instanceof BaseTreeItem) {
			((BaseTreeItem) child).expandParents();
		} else {
			child.setExpanded(false);
		}
	}

	@Override
	public int compareTo(BaseTreeItem other) {
		ItemType iDirectory = getValue().getItemType();
		ItemType oDirectory = other.getValue().getItemType();
		// Ensure directories are first
		if (iDirectory.ordinal() > oDirectory.ordinal())
			return -1;
		else if (iDirectory.ordinal() < oDirectory.ordinal())
			return 1;
		// Sort based on name
		String iPath = getValue().getPathElementValue();
		String oPath = other.getValue().getPathElementValue();
		if (iPath == null || oPath == null) {
			return getClass().getName().compareTo(other.getClass().getName());
		}
		return iPath.compareTo(oPath);
	}

	protected static BaseTreeItem addPath(BaseTreeItem item, String name,
								  Function<String, BaseTreeItem> leafFunction,
								  Function<String, BaseTreeItem> branchFunction) {
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		// Prune tree directory middle section if it is obnoxiously long
		int maxDepth = Configs.display().maxTreeDirectoryDepth;
		if (maxDepth > 0 && parts.size() > maxDepth) {
			String lastPart = parts.get(parts.size() - 1);
			// We keep only elements between [0 ... maxDepth-1] and the last part
			parts = new ArrayList<>(parts.subList(0, maxDepth - 1));
			parts.add(FLATTENED_ITEM);
			parts.add(lastPart);
		}
		// Build directory structure
		int maxLen = Configs.display().maxTreeTextLength;
		while (!parts.isEmpty()) {
			String part = parts.remove(0);
			if (part.length() > maxLen)
				part = part.substring(0, maxLen) + "...";
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			if (child == null) {
				child = isLeaf ?
						leafFunction.apply(name) :
						branchFunction.apply(part);
				item.addChild(child);
			}
			item = child;
		}
		return item;
	}

	protected static void remove(BaseTreeItem root, String name) {
		BaseTreeItem item = root;
		BaseTreeItem parent = root;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while (!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			// Should not be null if the tree has the item denoted by the given path (split into parts)
			if (child == null) {
				// Since we flatten some deep directory structures we need to handle the edge case where we've
				// dropped all items but the last one.
				child = item.getChildDirectory(FLATTENED_ITEM);
				if (child == null) {
					return;
				} else {
					// Drop all parts of the path except the last one
					parts = parts.subList(parts.size() - 1, parts.size());
				}
			}
			parent = item;
			item = child;
		}
		// Remove child from parent.
		// If parent is now empty, remove it as well.
		do {
			parent.removeChild(item);
			item = parent;
			parent = (BaseTreeItem) item.getParent();
		} while (item.isLeaf());
	}
}
