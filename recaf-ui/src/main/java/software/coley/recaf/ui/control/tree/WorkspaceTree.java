package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
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
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tree view for navigating a {@link Workspace}.
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceTree extends PathNodeTree implements WorkspaceCloseListener {
	private static final Logger logger = Logging.get(WorkspaceTree.class);
	private final WorkspaceExplorerConfig explorerConfig;
	private final ExecutorService treeBuildPool = ThreadPoolFactory.newSingleThreadExecutor("workspace-tree-build");
	private final AtomicInteger buildGeneration = new AtomicInteger();
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
		int generation = buildGeneration.incrementAndGet();

		// Remove listeners on old workspace root node
		if (root != null)
			root.removeWorkspaceListeners();

		// Update workspace reference & clear the current root while a new one is built.
		this.workspace = workspace;
		root = null;
		FxThreadUtil.run(() -> {
			if (generation == buildGeneration.get())
				setRoot(null);
		});

		if (workspace != null) {
			// Build a detached root in the background. Some large inputs can take a second or two to build.
			WorkspacePathNode rootPath = PathNodes.workspacePath(workspace);
			treeBuildPool.execute(() -> {
				try {
					WorkspaceRootTreeNode builtRoot = new WorkspaceRootTreeNode(explorerConfig, rootPath);
					builtRoot.build();
					FxThreadUtil.run(() -> {
						// Skip if the workspace was changed while building, or if the workspace was cleared.
						if (generation != buildGeneration.get() || this.workspace != workspace)
							return;

						root = builtRoot;
						root.addWorkspaceListeners();
						setRoot(root);
					});
				} catch (Throwable t) {
					logger.error("Failed building workspace tree", t);
				}
			});
		}
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		// Workspace closed, disable tree.
		if (root != null && root.isTargetWorkspace(workspace))
			FxThreadUtil.run(() -> setDisable(true));
	}

	@PreDestroy
	private void destroy() {
		// Invalidate any pending builds, and stop the build thread.
		buildGeneration.incrementAndGet();
		treeBuildPool.shutdownNow();
		if (root != null)
			root.removeWorkspaceListeners();
	}
}
