package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.ui.control.tree.TreeFiltering;
import software.coley.recaf.ui.control.tree.WorkspaceTree;
import software.coley.recaf.ui.control.tree.WorkspaceTreeFilterPane;
import software.coley.recaf.ui.dnd.DragAndDrop;
import software.coley.recaf.ui.dnd.WorkspaceLoadingDropListener;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Pane to display the current workspace in a navigable tree layout.
 *
 * @author Matt Coley
 * @see WorkspaceRootPane
 */
@Dependent
public class WorkspaceExplorerPane extends BorderPane {
	private final WorkspaceTree workspaceTree;

	/**
	 * @param listener
	 * 		Workspace drag-and-drop listener.
	 * @param workspaceTree
	 * 		Tree to display workspace with.
	 * @param workspace
	 * 		Current workspace, if any.
	 */
	@Inject
	public WorkspaceExplorerPane(@Nonnull WorkspaceLoadingDropListener listener,
								 @Nonnull WorkspaceTree workspaceTree,
								 @Nullable Workspace workspace) {
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
		setCenter(workspaceTree);
		setBottom(workspaceTreeFilterPane);

		// Populate tree
		if (workspace != null)
			workspaceTree.createWorkspaceRoot(workspace);
	}

	/**
	 * @return Tree displaying a workspace.
	 */
	@Nonnull
	public WorkspaceTree getWorkspaceTree() {
		return workspaceTree;
	}
}
