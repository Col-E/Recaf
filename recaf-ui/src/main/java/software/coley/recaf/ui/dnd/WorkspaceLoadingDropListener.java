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

		if (config.createOnDragDrop()) {
			// Create new workspace from files
			//  - The last path is actually the first file selected by the user when multiple files are selected
			Path primary = files.getLast();
			List<Path> supporting = files.size() > 1 ? files.subList(1, files.size()) : Collections.emptyList();
			pathLoadingManager.asyncNewWorkspace(primary, supporting, err -> {
				logger.error("Failed to create new workspace from dropped files", err);
			});
		} else if (workspaceManager.getCurrent() != null && config.appendOnDragDrop()) {
			// Append files to current workspace
			pathLoadingManager.asyncAddSupportingResourcesToWorkspace(workspaceManager.getCurrent(), files, err -> {
				logger.error("Failed to add supporting resources from dropped files", err);
			});
		}
	}
}
