package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.Unchecked;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic workspace comment container for persistence.
 *
 * @author Matt Coley
 */
public class PersistWorkspaceComments implements WorkspaceComments {
	private final Map<String, PersistClassComments> classCommentsMap = new ConcurrentHashMap<>();
	private String workspaceKey;

	public PersistWorkspaceComments(@Nonnull String workspaceKey) {
		this.workspaceKey = workspaceKey;
	}

	/**
	 * @return The {@link CommentKey#workspaceInput(Workspace)} key of the associated workspace.
	 */
	@Nonnull
	public String getWorkspaceKey() {
		// This value may be null from deserialization if migrated from an older version where this field was not present.
		if (workspaceKey == null)
			workspaceKey = "legacy-" + UUID.randomUUID();
		return workspaceKey;
	}

	/**
	 * @return Names of classes with comment containers.
	 */
	@Nonnull
	Collection<String> classKeys() {
		// The class keys are exposed so that the comment manager can copy state over to the delegate models.
		return classCommentsMap.keySet();
	}

	@Nonnull
	@Override
	public ClassComments getOrCreateClassComments(@Nonnull ClassPathNode classPath) {
		return classCommentsMap.computeIfAbsent(classPath.getValue().getName(), name -> new PersistClassComments());
	}

	@Nullable
	@Override
	public ClassComments getClassComments(@Nonnull ClassPathNode classPath) {
		return classCommentsMap.get(classPath.getValue().getName());
	}

	@Nullable
	@Override
	public ClassComments deleteClassComments(@Nonnull ClassPathNode classPath) {
		return classCommentsMap.remove(classPath.getValue().getName());
	}

	@Nonnull
	@Override
	public Iterator<ClassComments> iterator() {
		return Unchecked.cast(classCommentsMap.values().iterator());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PersistWorkspaceComments that = (PersistWorkspaceComments) o;

		return classCommentsMap.equals(that.classCommentsMap);
	}

	@Override
	public int hashCode() {
		return classCommentsMap.hashCode();
	}

	@Override
	public String toString() {
		return getWorkspaceKey() + " [" + classCommentsMap.size() + " classes]";
	}
}
