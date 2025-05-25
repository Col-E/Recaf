package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.tree.TreeFiltering;
import software.coley.recaf.ui.control.tree.WorkspaceTree;
import software.coley.recaf.ui.control.tree.WorkspaceTreeFilterPane;
import software.coley.recaf.ui.dnd.DragAndDrop;
import software.coley.recaf.ui.dnd.WorkspaceLoadingDropListener;
import software.coley.recaf.ui.docking.DockingLayoutManager;
import software.coley.recaf.util.Lang;

/**
 * Pane to display the current workspace in a navigable tree layout.
 *
 * @author Matt Coley
 * @see DockingLayoutManager
 */
@Dependent
public class WorkspaceExplorerPane extends BorderPane {
	private final WorkspaceTree workspaceTree;

	/**
	 * @param listener
	 * 		Workspace drag-and-drop listener.
	 * @param workspaceTree
	 * 		Tree to display workspace with.
	 * @param workspaceManager
	 * 		Manager to pull in current workspace from.
	 */
	@Inject
	public WorkspaceExplorerPane(@Nonnull WorkspaceLoadingDropListener listener,
	                             @Nonnull WorkspaceTree workspaceTree,
	                             @Nonnull WorkspaceManager workspaceManager) {
		this.workspaceTree = workspaceTree;

		// As we are the explorer pane, these items should be treated as declarations and not references.
		workspaceTree.contextSourceObjectPropertyProperty().setValue(ContextSource.DECLARATION);

		// Add filter pane, and hook up key-events so the user can easily
		// navigate between the tree and the filter.
		WorkspaceTreeFilterPane workspaceTreeFilterPane = new WorkspaceTreeFilterPane(workspaceTree);
		TreeFiltering.install(workspaceTreeFilterPane.getTextField(), workspaceTree);

		// Initialize drag-drop support.
		DragAndDrop.installFileSupport(this, listener);

		// Layout
		StackPane stack = new StackPane(workspaceTree);
		setCenter(stack);
		setBottom(workspaceTreeFilterPane);

		// Populate tree
		if (workspaceManager.hasCurrentWorkspace())
			workspaceTree.createWorkspaceRoot(workspaceManager.getCurrent());

		// Add label to indicate when filter pane input results in the tree being empty.
		// This should help out users if they forget they have something in the search bar and the tree looks empty.
		Label noResultsLabel = new BoundLabel(Lang.getBinding("menu.search.noresults"));
		noResultsLabel.setGraphic(new FontIconView(CarbonIcons.SEARCH));
		noResultsLabel.setMouseTransparent(true);
		noResultsLabel.setVisible(false);
		noResultsLabel.setOpacity(0.5);
		workspaceTreeFilterPane.currentPredicateProperty().addListener((ob, old, cur) -> {
			TreeItem<PathNode<?>> root = workspaceTree.getRoot();
			if (root != null) {
				noResultsLabel.setVisible(root.getChildren().isEmpty());
			} else {
				noResultsLabel.setVisible(false);
			}
		});
		StackPane.setAlignment(noResultsLabel, Pos.CENTER);
		stack.getChildren().add(noResultsLabel);
	}

	/**
	 * @return Tree displaying a workspace.
	 */
	@Nonnull
	public WorkspaceTree getWorkspaceTree() {
		return workspaceTree;
	}
}
