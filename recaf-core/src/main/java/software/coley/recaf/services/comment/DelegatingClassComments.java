package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;

import java.time.Instant;

/**
 * Delegating class comment container implementation.
 *
 * @author Matt Coley
 */
public class DelegatingClassComments implements ClassComments {
	private final CommentUpdateListener listenerCallback;
	private final ClassPathNode path;
	private final ClassComments delegate;

	/**
	 * New delegating comments container, which passes data to the persistence container,
	 * and invokes {@link CommentUpdateListener} methods when comment data is updated.
	 *
	 * @param path
	 * 		Path to the class this container is for.
	 * @param listenerCallback
	 * 		The listener that delegates to other listeners registered in the {@link CommentManager}.
	 * @param delegate
	 * 		The {@link ClassComments} implementation we actually want to store data in.
	 */
	public DelegatingClassComments(@Nonnull ClassPathNode path, @Nonnull CommentUpdateListener listenerCallback, @Nonnull ClassComments delegate) {
		this.listenerCallback = listenerCallback;
		this.path = path;
		this.delegate = delegate;
	}

	/**
	 * @return Path to the class this container is for.
	 */
	@Nonnull
	public ClassPathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Instant getCreationTime() {
		return delegate.getCreationTime();
	}

	@Nonnull
	@Override
	public Instant getLastUpdatedTime() {
		return delegate.getLastUpdatedTime();
	}

	@Override
	public boolean hasComments() {
		return delegate.hasComments();
	}

	@Nullable
	@Override
	public String getClassComment() {
		return delegate.getClassComment();
	}

	@Override
	public void setClassComment(@Nullable String comment) {
		delegate.setClassComment(comment);

		listenerCallback.onClassCommentUpdated(path, comment);
	}

	@Nullable
	@Override
	public String getFieldComment(@Nonnull String name, @Nonnull String descriptor) {
		return delegate.getFieldComment(name, descriptor);
	}

	@Nullable
	@Override
	public String getMethodComment(@Nonnull String name, @Nonnull String descriptor) {
		return delegate.getMethodComment(name, descriptor);
	}

	@Override
	public void setFieldComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment) {
		delegate.setFieldComment(name, descriptor, comment);

		FieldMember field = path.getValue().getDeclaredField(name, descriptor);
		if (field != null)
			listenerCallback.onFieldCommentUpdated(path.child(field), comment);
	}

	@Override
	public void setMethodComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment) {
		delegate.setMethodComment(name, descriptor, comment);

		MethodMember method = path.getValue().getDeclaredMethod(name, descriptor);
		if (method != null)
			listenerCallback.onMethodCommentUpdated(path.child(method), comment);
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

	@Override
	public String toString() {
		return delegate.toString();
	}
}
