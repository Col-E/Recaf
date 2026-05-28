package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

/**
 * Discovery process for locating entry points in a single workspace resource.
 *
 * @author Matt Coley
 */
public interface EntryPointDiscovery extends PrioritySortable {
	/**
	 * @return Kind of entry points emitted by the discovery.
	 */
	@Nonnull
	EntryPointKind kind();

	/**
	 * Discover entry points in the given resource.
	 * Implementations should only inspect the passed resource and must not recurse into embedded resources.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect.
	 *
	 * @return Discovered entry points.
	 */
	@Nonnull
	List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource);
}
