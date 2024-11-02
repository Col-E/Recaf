package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import software.coley.collections.Unchecked;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.util.StringUtil;

/**
 * Tree item subtype for more convenience tree building operations.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeNode extends FilterableTreeItem<PathNode<?>> implements Comparable<WorkspaceTreeNode> {
	/**
	 * Create new node with path value.
	 *
	 * @param path
	 * 		Path of represented item.
	 */
	public WorkspaceTreeNode(PathNode<?> path) {
		setValue(path);
	}

	/**
	 * Removes a tree node from the tree by its {@link PathNode} equality.
	 *
	 * @param path
	 * 		Path to remove from the tree.
	 *
	 * @return {@code true} when removal is a success.
	 * {@code false} if nothing was removed.
	 */
	public synchronized boolean removeNodeByPath(@Nonnull PathNode<?> path) {
		// Get node by path from the root.
		WorkspaceTreeNode nodeByPath = getRoot().getNodeByPath(path);

		// Get that node's parent, remove the child.
		if (nodeByPath != null) {
			WorkspaceTreeNode parentNode = nodeByPath.getSourceParentNode();
			if (parentNode != null) {
				boolean removed = parentNode.removeSourceChild(nodeByPath);
				while (parentNode.isSourceLeaf() && parentNode.getSourceParentNode() != null) {
					WorkspaceTreeNode parentOfParent = parentNode.getSourceParentNode();
					parentOfParent.removeSourceChild(parentNode);
					parentNode = parentOfParent;
				}
				return removed;
			}
		}

		// No known node by path.
		return false;
	}

	/**
	 * Gets or creates a tree node by the given {@link PathNode}.
	 *
	 * @param path
	 * 		Path associated with node to look for in tree.
	 *
	 * @return Node containing the path in the tree.
	 */
	@Nonnull
	public synchronized WorkspaceTreeNode getOrCreateNodeByPath(@Nonnull PathNode<?> path) {
		// Call from root node only.
		WorkspaceTreeNode root = getRoot();

		// Lookup and/or create nodes for path.
		return getOrInsertIntoTree(root, path);
	}

	/**
	 * Searches for a {@link WorkspaceTreeNode} item in the tree model, matching the given path.
	 *
	 * @param path
	 * 		Path associated with node to look for in tree.
	 *
	 * @return Node containing the path in the tree.
	 */
	@Nullable
	public synchronized WorkspaceTreeNode getNodeByPath(@Nonnull PathNode<?> path) {
		// Base case, we are that path.
		PathNode<?> value = getValue();
		if (path.equals(value))
			return this;

		// Check all children for a match, regardless of the current filter.
		for (TreeItem<PathNode<?>> child : getSourceChildren())
			if (path.isDescendantOf(child.getValue()) && child instanceof WorkspaceTreeNode childNode)
				return childNode.getNodeByPath(path);

		return null;
	}

	/**
	 * @return First child tree node. {@code null} if no child is found.
	 */
	@Nullable
	public synchronized WorkspaceTreeNode getFirstChild() {
		// Get first child, regardless of the current filter.
		var children = getSourceChildren();
		return children.isEmpty()
				? null : children.getFirst() instanceof WorkspaceTreeNode node
				? node : null;
	}

	/**
	 * @return The root of this tree node's parent hierarchy.
	 */
	@Nonnull
	public WorkspaceTreeNode getRoot() {
		WorkspaceTreeNode root = this;
		while (true) {
			WorkspaceTreeNode parentNode = root.getSourceParentNode();
			if (parentNode == null)
				break;
			root = parentNode;
		}
		return root;
	}

	/**
	 * @param path
	 * 		Path to check against.
	 *
	 * @return {@code true} when the current node's path matches.
	 */
	public boolean matches(@Nonnull PathNode<?> path) {
		return path.equals(getValue());
	}

	/**
	 * @return {@link #getSourceParent()} but cast to {@link WorkspaceTreeNode}.
	 */
	@Nullable
	public WorkspaceTreeNode getSourceParentNode() {
		return (WorkspaceTreeNode) getSourceParent();
	}

	@Override
	public int compareTo(@Nonnull WorkspaceTreeNode o) {
		return getValue().compareTo(o.getValue());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getValue().toString() + "]";
	}

	/**
	 * A debugging util to inspect the state of the tree without having to dig through
	 * the actual references in the debugger.
	 *
	 * @return String representation of this tree and all of its descendants.
	 */
	@Nonnull
	public String printTree() {
		StringBuilder sb = new StringBuilder(toString());
		for (TreeItem<PathNode<?>> child : getSourceChildren()) {
			if (child instanceof WorkspaceTreeNode childNode) {
				String childTree = childNode.printTree();
				for (String childTreeEntry : StringUtil.fastSplit(childTree, false, '\n')) {
					sb.append("\n    ").append(childTreeEntry);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Get/insert a {@link WorkspaceTreeNode} holding the given {@link PathNode} from/to the tree model.
	 *
	 * @param node
	 * 		Tree node to insert into.
	 * @param path
	 * 		Path to insert, relative to the given node.
	 *
	 * @return Inserted node.
	 */
	@Nonnull
	public static WorkspaceTreeNode getOrInsertIntoTree(@Nonnull WorkspaceTreeNode node, @Nonnull PathNode<?> path) {
		return getOrInsertIntoTree(node, path, false);
	}

	/**
	 * Get/insert a {@link WorkspaceTreeNode} holding the given {@link PathNode} from/to the tree model.
	 *
	 * @param node
	 * 		Tree node to insert into.
	 * @param path
	 * 		Path to insert, relative to the given node.
	 * @param sorted
	 *        {@code true} if the path insertion is being done in a pre-sorted manner, typically in the case of building an initial tree.
	 *        {@code false} if the path being inserted is not guaranteed to be in order relative to the last path inserted into the tree.
	 *
	 * @return Inserted node.
	 */
	@Nonnull
	@SuppressWarnings("deprecation")
	public static WorkspaceTreeNode getOrInsertIntoTree(@Nonnull WorkspaceTreeNode node, @Nonnull PathNode<?> path, boolean sorted) {
		// Edge case handling for directory nodes.
		if (path instanceof DirectoryPathNode directoryPath) {
			// If we have parent links in our path, insert those first.
			// We should generate up to whatever context our parent is.
			BundlePathNode parent = directoryPath.getParent();
			if (parent != null)
				node = getOrInsertIntoTree(node, parent, sorted);

			// Work off of the first node that does NOT contain a directory value.
			// This should result in the node pointing to a bundle.
			while (node.getValue() instanceof DirectoryPathNode) {
				node = (WorkspaceTreeNode) node.getSourceParent();
				if (node == null)
					throw new IllegalStateException("Directory path node had no parent in workspace tree");
			}

			// Insert the directory path, separated by '/'.
			// Update 'node' as we build/fetch the directory path items.
			// We use '-1' as a limit in split to allow empty directories to be split properly:
			//  '//' --> ['', '', '']
			String fullDirectory = directoryPath.getValue();
			String[] directoryParts = fullDirectory.split("/", -1);

			StringBuilder directoryBuilder = new StringBuilder();
			for (String directoryPart : directoryParts) {
				// Build up directory path.
				directoryBuilder.append(directoryPart).append('/');
				String directoryName = directoryBuilder.substring(0, directoryBuilder.length() - 1);
				DirectoryPathNode localPathNode = directoryPath.withDirectory(directoryName);

				// Get existing tree node, or create child if non-existent
				WorkspaceTreeNode childNode = null;
				ObservableList<TreeItem<PathNode<?>>> children;
				if (node instanceof FilterableTreeItem<?> filterableNode)
					children = Unchecked.cast(filterableNode.getSourceChildren());
				else
					children = node.getChildren();
				for (TreeItem<PathNode<?>> child : children)
					if (child.getValue().equals(localPathNode)) {
						childNode = (WorkspaceTreeNode) child;
						break;
					}
				if (childNode == null) {
					childNode = new WorkspaceTreeNode(localPathNode);
					if (sorted) {
						node.addPreSortedChild(childNode);
					} else {
						node.addAndSortChild(childNode);
					}
				}

				// Prepare for next directory path entry.
				node = childNode;
			}
			return node;
		}

		// If we have parent links in our path, insert those first.
		// We should generate up to whatever context our parent is.
		PathNode<?> parent = path.getParent();
		if (parent != null)
			node = getOrInsertIntoTree(node, parent, sorted);
		else if (path.typeIdMatch(node.getValue())) {
			// We are the root link in the path. This check ensures that as the root type we do not
			// insert a new tree-node of the same value, to the children list of the root tree node.
			return node;
		}

		// Check if already inserted.
		ObservableList<TreeItem<PathNode<?>>> children;
		if (node instanceof FilterableTreeItem<?> filterableNode)
			children = Unchecked.cast(filterableNode.getSourceChildren());
		else
			children = node.getChildren();
		for (TreeItem<PathNode<?>> child : children)
			if (path.equals(child.getValue()))
				return (WorkspaceTreeNode) child;

		// Not already inserted, create a new node and insert it.
		WorkspaceTreeNode inserted = new WorkspaceTreeNode(path);
		if (sorted) {
			node.addPreSortedChild(inserted);
		} else {
			node.addAndSortChild(inserted);
		}
		return inserted;
	}
}
