package software.coley.recaf.ui.control.richtext.suggest.java.typeindex;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.util.concurrent.TimeUnit;

/**
 * Shared workspace-backed type index for Java source tab completion.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JavaTypeIndexService {
	private final Cache<Workspace, CacheEntry> cache = CacheBuilder.newBuilder()
			.weakKeys() // Intended for the side effect of using '==' for key comparisons over '.equals()'
			.maximumSize(1) // Users will only ever operate on one workspace, so this is free pruning when they switch.
			.expireAfterWrite(20, TimeUnit.MINUTES)
			.build();

	@Inject
	public JavaTypeIndexService(@Nonnull WorkspaceManager workspaceManager) {
		workspaceManager.addWorkspaceCloseListener(this::removeWorkspace);
	}

	/**
	 * @param workspace
	 * 		Workspace to pull types from.
	 *
	 * @return Current type index snapshot for the workspace.
	 */
	@Nonnull
	public TypeIndex getIndex(@Nonnull Workspace workspace) {
		synchronized (cache) {
			CacheEntry entry = cache.getIfPresent(workspace);
			if (entry == null) {
				entry = new CacheEntry(workspace);
				cache.put(workspace, entry);
			}
			return entry.getIndex();
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to remove from the cache.
	 */
	private void removeWorkspace(@Nonnull Workspace workspace) {
		synchronized (cache) {
			CacheEntry entry = cache.getIfPresent(workspace);
			if (entry != null) {
				cache.invalidate(workspace);
				entry.close();
			}
		}
	}

	/**
	 * @return Number of cached workspaces.
	 */
	@VisibleForTesting
	public int cachedWorkspaceCount() {
		synchronized (cache) {
			return Math.toIntExact(cache.size());
		}
	}
}
