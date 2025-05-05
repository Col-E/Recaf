package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.TreeItem;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.NodeEvents;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Tree view for navigating a {@link Workspace}.
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceTree extends PathNodeTree implements WorkspaceCloseListener {
	private final WorkspaceExplorerConfig explorerConfig;
	private WorkspaceRootTreeNode root;
	private Workspace workspace;

	/**
	 * Initialize empty tree.
	 *
	 * @param configurationService
	 * 		Service to configure cell content.
	 */
	@Inject
	public WorkspaceTree(@Nonnull CellConfigurationService configurationService, @Nonnull Actions actions,
	                     @Nonnull KeybindingConfig keys, @Nonnull WorkspaceExplorerConfig explorerConfig) {
		super(configurationService, actions);

		this.explorerConfig = explorerConfig;

		// Additional workspace-explorer specific bind handling
		NodeEvents.addKeyPressHandler(this, e -> {
			if (keys.getRename().match(e)) {
				TreeItem<PathNode<?>> selectedItem = getSelectionModel().getSelectedItem();
				if (selectedItem != null)
					actions.rename(selectedItem.getValue());
			}
		});
	}

	/**
	 * Sets the workspace, and creates a complete model for it.
	 *
	 * @param workspace
	 * 		Workspace to represent.
	 */
	public void createWorkspaceRoot(@Nullable Workspace workspace) {
		Workspace oldWorkspace = this.workspace;
		if (oldWorkspace != null && root != null) {
			// Remove listeners on old workspace
			oldWorkspace.removeWorkspaceModificationListener(root);
			for (WorkspaceResource resource : oldWorkspace.getAllResources(false))
				resource.removeListener(root);
		}

		// Update workspace reference & populate root.
		this.workspace = workspace;
		if (workspace == null) {
			root = null;
		} else {
			// Create root
			WorkspacePathNode rootPath = PathNodes.workspacePath(workspace);
			root = new WorkspaceRootTreeNode(explorerConfig, rootPath);
			root.build();
			root.addWorkspaceListeners();
		}
		FxThreadUtil.run(() -> setRoot(root));
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		// Workspace closed, disable tree.
		if (root.isTargetWorkspace(workspace))
			FxThreadUtil.run(() -> setDisable(true));
	}
}
