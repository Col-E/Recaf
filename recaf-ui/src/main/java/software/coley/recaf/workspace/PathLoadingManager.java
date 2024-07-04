package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.io.ResourceImporter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
	private static final Logger logger = Logging.get(PathLoadingManager.class);
	private final ExecutorService loadPool = ThreadPoolFactory.newSingleThreadExecutor("path-loader");
	private final List<WorkspacePreLoadListener> preLoadListeners = new CopyOnWriteArrayList<>();
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
	public void addPreLoadListener(@Nonnull WorkspacePreLoadListener listener) {
		preLoadListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removePreLoadListener(@Nonnull WorkspacePreLoadListener listener) {
		preLoadListeners.remove(listener);
	}

	/**
	 * @param primaryPath
	 * 		Path to the primary resource file.
	 * @param supportingPaths
	 * 		Paths to the supporting resource files.
	 * @param errorHandling
	 * 		Error handling for invalid input.
	 *
	 * @return Future of the created workspace.
	 */
	@Nonnull
	public CompletableFuture<Workspace> asyncNewWorkspace(@Nonnull Path primaryPath, @Nonnull List<Path> supportingPaths,
	                                                      @Nonnull Consumer<Throwable> errorHandling) {
		// Invoke listeners, new content is being loaded.
		Unchecked.checkedForEach(preLoadListeners,
				listener -> listener.onPreLoad(primaryPath, supportingPaths),
				(listener, t) -> logger.error("Exception thrown opening workspace from '{}'", primaryPath, t));

		// Load resources from paths.
		CompletableFuture<Workspace> future = new CompletableFuture<>();
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
				future.complete(workspace);
				workspaceManager.setCurrent(workspace);
			} catch (Throwable t) {
				future.completeExceptionally(t);
				errorHandling.accept(t);
			}
		});
		return future;
	}

	/**
	 * @param workspace
	 * 		Workspace to add to.
	 * @param supportingPaths
	 * 		Paths to the supporting resource files.
	 * @param errorHandling
	 * 		Error handling for invalid input.
	 *
	 * @return Future of added supporting resources.
	 */
	@Nonnull
	public CompletableFuture<List<WorkspaceResource>> asyncAddSupportingResourcesToWorkspace(@Nonnull Workspace workspace,
	                                                                                         @Nonnull List<Path> supportingPaths,
	                                                                                         @Nonnull Consumer<IOException> errorHandling) {
		// Load resources from paths.
		CompletableFuture<List<WorkspaceResource>> future = new CompletableFuture<>();
		loadPool.submit(() -> {
			try {
				List<WorkspaceResource> loadedResources = new ArrayList<>(supportingPaths.size());
				for (Path supportingPath : supportingPaths) {
					WorkspaceResource supportResource = resourceImporter.importResource(supportingPath);
					loadedResources.add(supportResource);
					workspace.addSupportingResource(supportResource);
				}
				future.complete(loadedResources);
			} catch (IOException ex) {
				future.completeExceptionally(ex);
				errorHandling.accept(ex);
			}
		});
		return future;
	}
}
