package software.coley.recaf.ui.pane.editing.tabs;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.BoundMultiToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Displays parents and children of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class InheritancePane extends StackPane implements UpdatableNavigable {
	private final SimpleObjectProperty<TreeContent> contentType = new SimpleObjectProperty<>(TreeContent.CHILDREN);
	private final TreeView<PathNode<?>> tree = new TreeView<>();
	private final InheritanceGraph inheritanceGraph;
	private Workspace workspace;
	private ClassPathNode path;

	@Inject
	public InheritancePane(@Nonnull InheritanceGraphService graphService,
						   @Nonnull CellConfigurationService configurationService) {
		this.inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
		contentType.addListener((ob, old, cur) -> regenerateTree());

		// Configure tree.
		tree.setShowRoot(true);
		tree.setCellFactory(param -> new WorkspaceTreeCell(p -> Objects.equals(p, path) ?
				ContextSource.DECLARATION : ContextSource.REFERENCE, configurationService));
		tree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);

		// Configure toggle button between parent & child display.
		BoundMultiToggleIcon<TreeContent> toggle = new BoundMultiToggleIcon<>(TreeContent.class, contentType,
				c -> new FontIconView(c == TreeContent.CHILDREN ? CarbonIcons.ARROW_DOWN : CarbonIcons.ARROW_UP));
		toggle.textProperty().bind(contentType.map(c -> c == TreeContent.CHILDREN ?
				Lang.get("hierarchy.children") : Lang.get("hierarchy.parents")));
		toggle.getStyleClass().add(Styles.ROUNDED);
		toggle.setFocusTraversable(false);
		StackPane.setAlignment(toggle, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(toggle, new Insets(10));

		// Layout
		getChildren().addAll(tree, toggle);
		setMinWidth(235);
	}

	/**
	 * Recreate tree contents based on whether we want to show child types, or parent types.
	 */
	private void regenerateTree() {
		// Skip if path not set.
		if (path == null) {
			tree.setRoot(null);
			return;
		}

		// Skip if root not found in the graph.
		InheritanceVertex vertex = inheritanceGraph.getVertex(path.getValue().getName());
		if (vertex == null) {
			tree.setRoot(null);
			return;
		}

		// Create the appropriate tree model and assign it.
		WorkspaceTreeNode root = new WorkspaceTreeNode(path);
		if (contentType.get() == TreeContent.CHILDREN)
			createChildren(root, vertex);
		else
			createParents(root, vertex);
		tree.setRoot(root);
	}

	/**
	 * Adds child tree nodes representing the class's parent types.
	 *
	 * @param node
	 * 		Node to add children to.
	 * @param vertex
	 * 		Vertex to operate off of.
	 */
	private void createParents(@Nonnull WorkspaceTreeNode node, @Nonnull InheritanceVertex vertex) {
		node.setExpanded(true);
		for (InheritanceVertex parentVertex : vertex.getParents()) {
			if (parentVertex.isJavaLangObject())
				continue;
			ClassPathNode parentPath = workspace.findClass(parentVertex.getName());
			if (parentPath != null) {
				WorkspaceTreeNode subItem = new WorkspaceTreeNode(parentPath);
				if (noLoops(node, subItem)) {
					node.addAndSortChild(subItem);
					createParents(subItem, parentVertex);
				}
			}
		}
	}

	/**
	 * Adds child tree nodes representing the class's child types.
	 *
	 * @param node
	 * 		Node to add children to.
	 * @param vertex
	 * 		Vertex to operate off of.
	 */
	private void createChildren(@Nonnull WorkspaceTreeNode node, @Nonnull InheritanceVertex vertex) {
		node.setExpanded(true);
		for (InheritanceVertex childVertex : vertex.getChildren()) {
			ClassPathNode childPath = workspace.findClass(childVertex.getName());
			if (childPath != null) {
				WorkspaceTreeNode subItem = new WorkspaceTreeNode(childPath);
				if (noLoops(node, subItem)) {
					node.addAndSortChild(subItem);
					createChildren(subItem, childVertex);
				}
			}
		}
	}

	/**
	 * @param item
	 * 		Item to check against.
	 * @param child
	 * 		Item to add.
	 *
	 * @return {@code true} when no cycles are detected.
	 */
	private boolean noLoops(@Nullable TreeItem<?> item, @Nonnull TreeItem<?> child) {
		if (item == null)
			return true;
		if (item.getValue() == child.getValue())
			return false;
		return noLoops(item.getParent(), child);
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;
			workspace = path.getValueOfType(Workspace.class);
			regenerateTree();
		}
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
		tree.setRoot(null);
	}

	/**
	 * Enum for tree content options.
	 */
	public enum TreeContent {
		CHILDREN,
		PARENTS
	}
}
