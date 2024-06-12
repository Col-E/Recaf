package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.collections.Maps;
import software.coley.collections.Unchecked;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Path node for {@link WorkspaceResource} types.
 *
 * @author Matt Coley
 */
public class ResourcePathNode extends AbstractPathNode<Workspace, WorkspaceResource> {
	/**
	 * Type identifier for annotation nodes.
	 */
	public static final String TYPE_ID = "resource";

	/**
	 * Node without parent.
	 *
	 * @param resource
	 * 		Resource value.
	 */
	public ResourcePathNode(@Nonnull WorkspaceResource resource) {
		this((WorkspacePathNode) null, resource);
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
		super(TYPE_ID, parent, resource);
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
	public ResourcePathNode(@Nullable EmbeddedResourceContainerPathNode parent, @Nonnull WorkspaceResource resource) {
		super(TYPE_ID, parent, resource);
	}

	/**
	 * @param bundle
	 * 		Bundle to wrap into node.
	 *
	 * @return Path node of bundle, with the current resource as parent.
	 */
	@Nonnull
	public BundlePathNode child(@Nonnull Bundle<?> bundle) {
		return new BundlePathNode(this, bundle);
	}

	/**
	 * @return Path node for a container of multiple embedded resources.
	 */
	@Nonnull
	public EmbeddedResourceContainerPathNode embeddedChildContainer() {
		Workspace valueOfType = Objects.requireNonNull(getValueOfType(Workspace.class),
				"Path did not contain workspace in parent");
		return new EmbeddedResourceContainerPathNode(this, valueOfType);
	}

	/**
	 * @return {@code true} when this resource node, wraps the primary resource of a workspace.
	 */
	public boolean isPrimary() {
		PathNode<Workspace> parent = getParent();
		if (parent == null) return false;
		return parent.getValue().getPrimaryResource() == getValue();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(WorkspacePathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof ResourcePathNode resourcePathNode) {
			PathNode<Workspace> parent = getParent();
			Workspace workspace = parentValue();
			WorkspaceResource resource = getValue();
			WorkspaceResource otherResource = resourcePathNode.getValue();

			if (parent instanceof EmbeddedResourceContainerPathNode) {
				PathNode<WorkspaceResource> parentOfParent = Unchecked.cast(parent.getParent());
				Map<WorkspaceFileResource, String> lookup = Maps.reverse(parentOfParent.getValue().getEmbeddedResources());
				String ourKey = lookup.getOrDefault(resource, "?");
				String otherKey = lookup.getOrDefault(otherResource, "?");
				return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(ourKey, otherKey);
			} else {
				if (workspace != null) {
					if (resource == otherResource)
						return 0;

					// Show in order as in the workspace.
					List<WorkspaceResource> resources = workspace.getAllResources(false);
					return Integer.compare(resources.indexOf(resource), resources.indexOf(otherResource));
				} else {
					// Enforce some ordering. Not ideal but works.
					return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(
							resource.getClass().getSimpleName(),
							otherResource.getClass().getSimpleName()
					);
				}
			}
		}
		return 0;
	}
}
