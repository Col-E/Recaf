package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.io.ResourceImporter;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Manager module handle loading {@link Workspace} instances from {@link Path}s
 * and handling pushing events to tracked {@link WorkspacePreLoadListener} instances across the application.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PathLoadingManager {
	private final ExecutorService loadPool = ThreadPoolFactory.newSingleThreadExecutor("path-loader");
	private final List<WorkspacePreLoadListener> preLoadListeners = new ArrayList<>();
	private final WorkspaceManager workspaceManager;
	private final ResourceImporter resourceImporter;

	@Inject
	public PathLoadingManager(WorkspaceManager workspaceManager, ResourceImporter resourceImporter) {
		this.workspaceManager = workspaceManager;
		this.resourceImporter = resourceImporter;
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addPreLoadListener(WorkspacePreLoadListener listener) {
		preLoadListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removePreLoadListener(WorkspacePreLoadListener listener) {
		preLoadListeners.remove(listener);
	}

	/**
	 * @param primaryPath
	 * 		Path to the primary resource file.
	 * @param supportingPaths
	 * 		Paths to the supporting resource files.
	 * @param errorHandling
	 * 		Error handling for invalid input.
	 */
	public void asyncNewWorkspace(@Nonnull Path primaryPath, @Nonnull List<Path> supportingPaths,
								  @Nonnull Consumer<IOException> errorHandling) {
		// Invoke listeners, new content is being loaded.
		for (WorkspacePreLoadListener listener : preLoadListeners)
			listener.onPreLoad(primaryPath, supportingPaths);

		// Load resources from paths.
		loadPool.submit(() -> {
			try {
				List<WorkspaceResource> supportingResources = new ArrayList<>();
				WorkspaceResource primaryResource = resourceImporter.importResource(primaryPath);
				for (Path supportingPath : supportingPaths) {
					WorkspaceResource supportResource = resourceImporter.importResource(supportingPath);
					supportingResources.add(supportResource);
				}

				// Wrap into workspace and assign it
				Workspace workspace = new BasicWorkspace(primaryResource, supportingResources);
				workspaceManager.setCurrent(workspace);
			} catch (IOException ex) {
				errorHandling.accept(ex);
			}
		});
	}

	/**
	 * @param workspace
	 * 		Workspace to add to.
	 * @param supportingPaths
	 * 		Paths to the supporting resource files.
	 * @param errorHandling
	 * 		Error handling for invalid input.
	 */
	public void asyncAddSupportingResourcesToWorkspace(@Nonnull Workspace workspace,
													   @Nonnull List<Path> supportingPaths,
													   @Nonnull Consumer<IOException> errorHandling) {
		// Load resources from paths.
		loadPool.submit(() -> {
			try {
				for (Path supportingPath : supportingPaths) {
					WorkspaceResource supportResource = resourceImporter.importResource(supportingPath);
					workspace.addSupportingResource(supportResource);
				}
			} catch (IOException ex) {
				errorHandling.accept(ex);
			}
		});
	}
}
