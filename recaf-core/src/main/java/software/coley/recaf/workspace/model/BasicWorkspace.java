package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.resource.AndroidApiResource;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Basic workspace implementation.
 *
 * @author Matt Coley
 */
public class BasicWorkspace implements Workspace {
	private static final Logger logger = Logging.get(BasicWorkspace.class);
	private final List<WorkspaceModificationListener> modificationListeners = new CopyOnWriteArrayList<>();
	private final WorkspaceResource primary;
	private final List<WorkspaceResource> supporting = new ArrayList<>();
	private final List<WorkspaceResource> internal;

	/**
	 * @param primary
	 * 		Primary resource.
	 */
	public BasicWorkspace(@Nonnull WorkspaceResource primary) {
		this(primary, Collections.emptyList());
	}

	/**
	 * @param primary
	 * 		Primary resource.
	 * @param supporting
	 * 		Provided supporting resources.
	 */
	public BasicWorkspace(@Nonnull WorkspaceResource primary, @Nonnull Collection<WorkspaceResource> supporting) {
		this.primary = primary;
		this.supporting.addAll(supporting);

		RuntimeWorkspaceResource runtimeResource = RuntimeWorkspaceResource.getInstance();
		if (primary.getAndroidClassBundles().isEmpty()) {
			internal = Collections.singletonList(runtimeResource);
		} else {
			internal = List.of(runtimeResource, AndroidApiResource.getInstance());
		}
	}

	@Nonnull
	@Override
	public WorkspaceResource getPrimaryResource() {
		return primary;
	}

	@Nonnull
	@Override
	public List<WorkspaceResource> getSupportingResources() {
		return Collections.unmodifiableList(supporting);
	}

	@Nonnull
	@Override
	public List<WorkspaceResource> getInternalSupportingResources() {
		// Internal list is already unmodifiable, no need to wrap.
		return internal;
	}

	@Override
	public void addSupportingResource(@Nonnull WorkspaceResource resource) {
		supporting.add(resource);
		Unchecked.checkedForEach(modificationListeners, listener -> listener.onAddLibrary(this, resource),
				(listener, t) -> logger.error("Exception thrown when adding supporting resource", t));
	}

	@Override
	public boolean removeSupportingResource(@Nonnull WorkspaceResource resource) {
		boolean remove = supporting.remove(resource);
		if (remove) {
			Unchecked.checkedForEach(modificationListeners, listener -> listener.onRemoveLibrary(this, resource),
					(listener, t) -> logger.error("Exception thrown when removing supporting resource", t));
		}
		return remove;
	}

	@Nonnull
	@Override
	public List<WorkspaceModificationListener> getWorkspaceModificationListeners() {
		return Collections.unmodifiableList(modificationListeners);
	}

	@Override
	public void addWorkspaceModificationListener(@Nonnull WorkspaceModificationListener listener) {
		modificationListeners.add(listener);
	}

	@Override
	public void removeWorkspaceModificationListener(@Nonnull WorkspaceModificationListener listener) {
		modificationListeners.remove(listener);
	}

	/**
	 * Called by {@link WorkspaceManager} when the workspace is closed.
	 */
	@Override
	public void close() {
		modificationListeners.clear();
		supporting.forEach(Closing::close);
		primary.close();
	}

	@Override
	public String toString() {
		return "BasicWorkspace{" +
				"primary=" + primary +
				", supporting=" + supporting +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Workspace other = (Workspace) o;

		if (!primary.equals(other.getPrimaryResource())) return false;
		return supporting.equals(other.getSupportingResources());
	}

	@Override
	public int hashCode() {
		int result = primary.hashCode();
		result = 31 * result + supporting.hashCode();
		return result;
	}
}
