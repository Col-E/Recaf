package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;

/**
 * Path node for {@link Workspace} types.
 *
 * @author Matt Coley
 */
public class WorkspacePathNode extends AbstractPathNode<Object, Workspace> {
	/**
	 * Type identifier for workspace nodes.
	 */
	public static final String TYPE_ID = "workspace";

	/**
	 * Node without parent.
	 *
	 * @param value
	 * 		Workspace value.
	 */
	public WorkspacePathNode(@Nonnull Workspace value) {
		super(TYPE_ID, null, Workspace.class, value);
	}

	/**
	 * @param resource
	 * 		Resource to wrap into node.
	 *
	 * @return Path node of resource, with current workspace as parent.
	 */
	@Nonnull
	public ResourcePathNode child(@Nonnull WorkspaceResource resource) {
		return new ResourcePathNode(this, resource);
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Collections.emptySet();
	}

	@Override
	public boolean isDescendantOf(@Nonnull PathNode<?> other) {
		// Workspace is the root of all paths.
		// Only other workspace paths with the same value should count here.
		if (typeId().equals(other.typeId()))
			return getValue().equals(other.getValue());

		// We have no parents.
		return false;
	}

	@Override
	public int localCompare(PathNode<?> o) {
		return 0;
	}
}
