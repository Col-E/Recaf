package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caches the list of {@link IContextSource.Entry} items in a workspace.
 *
 * @author Matt Coley
 * @see LibrarySource
 */
public class WorkspaceEntriesCache {
	private List<IContextSource.Entry> cache;
	private int lastWorkspaceHash;

	/**
	 * @param workspace
	 * 		Workspace to get cached entries of.
	 *
	 * @return List of all distinctly named entries for classes in the workspace.
	 */
	@Nonnull
	public synchronized List<IContextSource.Entry> getCachedEntries(@Nonnull Workspace workspace) {
		List<IContextSource.Entry> local = cache;
		int workspaceHash = workspace.hashCode();
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
