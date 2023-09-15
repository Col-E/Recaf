package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Path node for {@link Workspace} types.
 *
 * @author Matt Coley
 */
public class WorkspacePathNode extends AbstractPathNode<Object, Workspace> {
	/**
	 * Node without parent.
	 *
	 * @param value
	 * 		Workspace value.
	 */
	public WorkspacePathNode(@Nonnull Workspace value) {
		super("workspace", null, Workspace.class, value);
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

	@Override
	public int localCompare(PathNode<?> o) {
		return 0;
	}
}
