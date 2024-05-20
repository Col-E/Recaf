package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Set;

/**
 * Path node for housing one or more embedded resources in another resource.
 *
 * @author Matt Coley
 */
public class EmbeddedResourceContainerPathNode extends AbstractPathNode<WorkspaceResource, Workspace> {
	/**
	 * Type identifier for embedded containers.
	 */
	public static final String TYPE_ID = "embedded-container";

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param workspace
	 * 		Workspace containing the host resource.
	 */
	public EmbeddedResourceContainerPathNode(@Nullable ResourcePathNode parent,
	                                         @Nonnull Workspace workspace) {
		super(TYPE_ID, parent, workspace);
	}

	/**
	 * @param resource
	 * 		Resource to wrap into node.
	 *
	 * @return Path node of resource, with the current workspace as parent.
	 */
	@Nonnull
	public ResourcePathNode child(@Nonnull WorkspaceResource resource) {
		return new ResourcePathNode(this, resource);
	}

	@Override
	public ResourcePathNode getParent() {
		return (ResourcePathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(EmbeddedResourceContainerPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		return 0;
	}
}
