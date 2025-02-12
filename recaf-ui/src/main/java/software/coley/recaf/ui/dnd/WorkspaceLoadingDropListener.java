package software.coley.recaf.ui.dnd;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.pane.WelcomePane;
import software.coley.recaf.ui.pane.WorkspaceExplorerPane;
import software.coley.recaf.workspace.PathLoadingManager;
import software.coley.recaf.services.workspace.WorkspaceManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Listener implementation to handle drag-and-drop to load workspace content.
 *
 * @author Matt Coley
 * @see WorkspaceExplorerPane Usage for when a workspace is already open.
 * @see WelcomePane Usage for when a user first starts Recaf.
 */
@ApplicationScoped
public class WorkspaceLoadingDropListener implements FileDropListener {
	private static final Logger logger = Logging.get(WorkspaceExplorerPane.class);
	private final WorkspaceExplorerConfig config;
	private final PathLoadingManager pathLoadingManager;
	private final WorkspaceManager workspaceManager;

	@Inject
	public WorkspaceLoadingDropListener(@Nonnull WorkspaceExplorerConfig config,
										@Nonnull PathLoadingManager pathLoadingManager,
										@Nonnull WorkspaceManager workspaceManager) {
		this.config = config;
		this.pathLoadingManager = pathLoadingManager;
		this.workspaceManager = workspaceManager;
	}

	@Override
	public void onDragDrop(@Nonnull Region region, @Nonnull DragEvent event, @Nonnull List<Path> files) throws IOException {
		// Sanity check input
		if (files.isEmpty()) return;

		if (config.createOnDragDrop() || !workspaceManager.hasCurrentWorkspace()) {
			// Windows sucks: https://superuser.com/questions/1696568/windows-explorer-file-order-in-the-clipboard
			//  - We can get the last clicked file to always be first when dragging from explorer
			//    but everything else will be in shuffled order, and we can't do anything about it.
			//  - Example: Using 'everything' instead of explorer for selection, we cannot guarantee drag-n-drop order.
			Path primary = files.getFirst();
			List<Path> supporting = files.size() > 1 ? files.subList(1, files.size()) : Collections.emptyList();
			pathLoadingManager.asyncNewWorkspace(primary, supporting, err -> {
				logger.error("Failed to create new workspace from dropped files", err);
			});
		} else if (workspaceManager.hasCurrentWorkspace() && config.appendOnDragDrop()) {
			// Append files to current workspace
			pathLoadingManager.asyncAddSupportingResourcesToWorkspace(workspaceManager.getCurrent(), files, err -> {
				logger.error("Failed to add supporting resources from dropped files", err);
			});
		}
	}
}
