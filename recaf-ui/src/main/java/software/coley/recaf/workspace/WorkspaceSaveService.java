package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.Info;
import software.coley.recaf.ui.config.ExportConfig;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Service for handling save operations in the user interface.
 * <p>
 * Optionally exports the workspace after a save, when {@link ExportConfig#getAutoExportAfterSave()} is enabled.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WorkspaceSaveService {
	private final ExportConfig exportConfig;
	private final PathExportingManager exportingManager;
	private final ExecutorService exportExecutor = ThreadPoolFactory.newSingleThreadExecutor("workspace-save-export");

	@Inject
	public WorkspaceSaveService(@Nonnull ExportConfig exportConfig,
	                            @Nonnull PathExportingManager exportingManager) {
		this.exportConfig = exportConfig;
		this.exportingManager = exportingManager;
	}

	/**
	 * Handle applying a save operation to the given contents of a workspace's bundle.
	 *
	 * @param workspace
	 * 		Workspace whose bundle is being saved.
	 * @param bundle
	 * 		Bundle to update.
	 * @param put
	 * 		Value to put.
	 * @param <I>
	 * 		Bundle value type.
	 *
	 * @return Completed future when auto-exporting is disabled.
	 * Otherwise, a future that completes after the optional export.
	 */
	@Nonnull
	public <I extends Info> CompletableFuture<Void> commit(@Nonnull Workspace workspace,
	                                                       @Nonnull Bundle<I> bundle,
	                                                       @Nonnull I put) {
		return commit(workspace, bundle, Collections.singletonList(put), Collections.emptyList());
	}

	/**
	 * Handle applying a save operation to the given contents of a workspace's bundle.
	 *
	 * @param workspace
	 * 		Workspace whose bundle is being saved.
	 * @param bundle
	 * 		Bundle to update.
	 * @param removedPaths
	 * 		Keys to remove in order.
	 * @param <I>
	 * 		Bundle value type.
	 *
	 * @return Completed future when auto-exporting is disabled.
	 * Otherwise, a future that completes after the optional export.
	 */
	@Nonnull
	public <I extends Info> CompletableFuture<Void> commitRemovals(@Nonnull Workspace workspace,
	                                                               @Nonnull Bundle<I> bundle,
	                                                               @Nonnull String... removedPaths) {
		return commit(workspace, bundle, Collections.emptyList(), Arrays.asList(removedPaths));
	}

	/**
	 * Handle applying a save operation to the given contents of a workspace's bundle.
	 *
	 * @param workspace
	 * 		Workspace whose bundle is being saved.
	 * @param bundle
	 * 		Bundle to update.
	 * @param puts
	 * 		Values to put in order.
	 * @param removals
	 * 		Keys to remove in order.
	 * @param <I>
	 * 		Bundle value type.
	 *
	 * @return Completed future when auto-exporting is disabled.
	 * Otherwise, a future that completes after the optional export.
	 */
	@Nonnull
	public <I extends Info> CompletableFuture<Void> commit(@Nonnull Workspace workspace,
	                                                       @Nonnull Bundle<I> bundle,
	                                                       @Nonnull Collection<? extends I> puts,
	                                                       @Nonnull Collection<String> removals) {
		// If there is nothing to do, skip the export step.
		if (puts.isEmpty() && removals.isEmpty())
			return CompletableFuture.completedFuture(null);

		// Apply the bundle changes synchronously on the caller's thread.
		for (I info : puts)
			bundle.put(info);
		for (String key : removals)
			bundle.remove(key);

		// If auto-exporting, run the export asynchronously on a separate thread.
		// Otherwise, we're already done.
		if (!exportConfig.getAutoExportAfterSave().getValue())
			return CompletableFuture.completedFuture(null);
		return CompletableFuture.runAsync(() -> {
			try {
				exportingManager.exportAutomatically(workspace);
			} catch (IOException ex) {
				try {
					exportingManager.reportWorkspaceExportFailure(null, ex);
				} catch (Throwable reportFailure) {
					ex.addSuppressed(reportFailure);
				}
				throw new CompletionException(ex);
			}
		}, exportExecutor);
	}

	@PreDestroy
	private void shutdown() {
		exportExecutor.shutdownNow();
	}
}