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
		super(TYPE_ID, null, value);
	}

	/**
	 * @param resource
	 * 		Resource to wrap into node.
	 *
	 * @return Path node of resource, with the current workspace as parent,
	 * or a {@link EmbeddedResourceContainerPathNode} if the passed resource is an embedded resource.
	 */
	@Nonnull
	public ResourcePathNode child(@Nonnull WorkspaceResource resource) {
		// Base case, resource is top-level in the workspace.
		WorkspaceResource containingResource = resource.getContainingResource();
		if (containingResource == null)
			return new ResourcePathNode(this, resource);

		// Resource is embedded, so we need to represent the path a bit differently.
		// - Note: We flatten the representation of embedded resources here.
		WorkspaceResource rootResource = containingResource;
		while (rootResource.getContainingResource() != null)
			rootResource = rootResource.getContainingResource();
		return new ResourcePathNode(this, rootResource)
				.embeddedChildContainer()
				.child(resource);
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

	@Override
	public boolean equals(Object o) {
		// Workspace equality checks are abysmally slow because are checking if all the contained
		// contents are also equal. Realistically we can get away with a reference check.
		if (o instanceof WorkspacePathNode otherPath)
			return getValue() == otherPath.getValue();
		return false;
	}
}
