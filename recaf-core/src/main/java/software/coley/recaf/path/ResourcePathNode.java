package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

/**
 * Path node for {@link WorkspaceResource} types.
 *
 * @author Matt Coley
 */
public class ResourcePathNode extends AbstractPathNode<Workspace, WorkspaceResource> {
	/**
	 * Node without parent.
	 *
	 * @param resource
	 * 		Resource value.
	 */
	public ResourcePathNode(@Nonnull WorkspaceResource resource) {
		this(null, resource);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param resource
	 * 		Resource value.
	 *
	 * @see WorkspacePathNode#child(WorkspaceResource)
	 */
	public ResourcePathNode(@Nullable WorkspacePathNode parent, @Nonnull WorkspaceResource resource) {
		super("resource", parent, WorkspaceResource.class, resource);
	}

	/**
	 * @param bundle
	 * 		Bundle to wrap into node.
	 *
	 * @return Path node of bundle, with current resource as parent.
	 */
	@Nonnull
	public BundlePathNode child(@Nonnull Bundle<?> bundle) {
		return new BundlePathNode(this, bundle);
	}

	/**
	 * @return {@code true} when this resource node, wraps the primary resource of a workspace.
	 */
	public boolean isPrimary() {
		WorkspacePathNode parent = getParent();
		if (parent == null) return false;
		return parent.getValue().getPrimaryResource() == getValue();
	}

	@Override
	public WorkspacePathNode getParent() {
		return (WorkspacePathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof ResourcePathNode resourcePathNode) {
			Workspace workspace = parentValue();
			WorkspaceResource resource = getValue();
			WorkspaceResource otherResource = resourcePathNode.getValue();
			if (workspace != null) {
				if (resource == otherResource)
					return 0;

				// Show in order as in the workspace.
				List<WorkspaceResource> resources = workspace.getAllResources(false);
				return Integer.compare(resources.indexOf(resource), resources.indexOf(otherResource));
			} else {
				// Enforce some ordering. Not ideal but works.
				return String.CASE_INSENSITIVE_ORDER.compare(
						resource.getClass().getSimpleName(),
						otherResource.getClass().getSimpleName()
				);
			}
		}
		return 0;
	}
}
