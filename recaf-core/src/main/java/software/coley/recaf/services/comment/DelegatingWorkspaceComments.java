package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.Unchecked;
import software.coley.recaf.path.ClassPathNode;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delegating workspace comment container implementation.
 *
 * @author Matt Coley
 */
public class DelegatingWorkspaceComments implements WorkspaceComments {
	private final Map<String, DelegatingClassComments> classCommentsMap = new ConcurrentHashMap<>();
	private final CommentManager listenerCallback;
	private final PersistWorkspaceComments delegate;

	/**
	 * New delegating comments container, which passes data to the persistence container, and manages
	 * the creation of {@link DelegatingClassComments} for backing {@link PersistClassComments} instances.
	 *
	 * @param listenerCallback
	 * 		The listener that delegates to other listeners registered in the {@link CommentManager}.
	 * @param delegate
	 * 		The {@link WorkspaceComments} implementation we actually want to store data in.
	 */
	public DelegatingWorkspaceComments(@Nonnull CommentManager listenerCallback, @Nonnull PersistWorkspaceComments delegate) {
		this.listenerCallback = listenerCallback;
		this.delegate = delegate;
	}

	@Nonnull
	@Override
	public ClassComments getOrCreateClassComments(@Nonnull ClassPathNode classPath) {
		// We will be delegating to the persist model when we lazily create our own delegating class comments here.
		// If the data does not exist upstream in the persist model, it will be made.
		return classCommentsMap.computeIfAbsent(classPath.getValue().getName(),
				name -> {
					DelegatingClassComments newComments = new DelegatingClassComments(classPath, listenerCallback, delegate.getOrCreateClassComments(classPath));
					listenerCallback.onClassContainerCreated(classPath, newComments);
					return newComments;
				});
	}

	@Nullable
	@Override
	public ClassComments getClassComments(@Nonnull ClassPathNode classPath) {
		// Check if the persist model has comments. If not, we do not need to make a wrapper.
		ClassComments delegateClassComments = delegate.getClassComments(classPath);
		if (delegateClassComments == null)
			return null;

		// Create a wrapper if one does not exist for the persist class comments model.
		return classCommentsMap.computeIfAbsent(classPath.getValue().getName(),
				name -> {
					DelegatingClassComments newComments = new DelegatingClassComments(classPath, listenerCallback, delegateClassComments);
					listenerCallback.onClassContainerCreated(classPath, newComments);
					return newComments;
				});
	}

	@Nullable
	@Override
	public ClassComments deleteClassComments(@Nonnull ClassPathNode classPath) {
		ClassComments container = delegate.deleteClassComments(classPath);
		if (container != null)
			listenerCallback.onClassContainerRemoved(classPath, container);
		return container;
	}

	@Nonnull
	@Override
	public Iterator<ClassComments> iterator() {
		return Unchecked.cast(classCommentsMap.values().iterator());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
}
