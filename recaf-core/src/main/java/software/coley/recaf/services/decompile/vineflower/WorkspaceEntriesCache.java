package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caches the list of {@link IContextSource.Entry} items in a workspace.
 *
 * @author Matt Coley
 * @see LibrarySource
 */
public class WorkspaceEntriesCache implements WorkspaceModificationListener, ResourceJvmClassListener {
	private List<IContextSource.Entry> cache;
	private int lastWorkspaceHash;

	@Override
	public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		resetCache();
	}

	@Override
	public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		resetCache();
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		resetCache();
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
		resetCache();
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		resetCache();
	}

	/**
	 * Clears the cache.
	 */
	private synchronized void resetCache() {
		cache = null;
	}

	/**
	 * @param workspace
	 * 		Workspace to get cached entries of.
	 *
	 * @return List of all distinctly named entries for classes in the workspace.
	 */
	@Nonnull
	public synchronized List<IContextSource.Entry> getCachedEntries(@Nonnull Workspace workspace) {
		List<IContextSource.Entry> local = cache;
		int workspaceHash = System.identityHashCode(workspace);
		if (local == null || workspaceHash != lastWorkspaceHash) {
			local = workspace.getAllResources(false).stream()
					.flatMap(WorkspaceResource::jvmAllClassBundleStreamRecursive)
					.flatMap(c -> c.keySet().stream())
					.distinct()
					.map(className -> new IContextSource.Entry(className, IContextSource.Entry.BASE_VERSION))
					.collect(Collectors.toList());
			cache = local;
			lastWorkspaceHash = workspaceHash;
		}
		return local;
	}
}
