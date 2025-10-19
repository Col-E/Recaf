package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic class comment container for persistence.
 *
 * @author Matt Coley
 */
public class PersistClassComments implements ClassComments {
	private final Map<String, String> fieldComments = new ConcurrentHashMap<>();
	private final Map<String, String> methodComments = new ConcurrentHashMap<>();
	private final Instant creationTime = Instant.now();
	private Instant lastUpdatedTime = creationTime;
	private String classComment;

	@Nonnull
	@Override
	public Instant getCreationTime() {
		return creationTime;
	}

	@Nonnull
	@Override
	public Instant getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	@Override
	public boolean hasComments() {
		return classComment != null
				|| fieldComments.values().stream().anyMatch(s -> s != null && !s.isBlank())
				|| methodComments.values().stream().anyMatch(s -> s != null && !s.isBlank());
	}

	@Nullable
	@Override
	public String getClassComment() {
		return classComment;
	}

	@Override
	public void setClassComment(@Nullable String comment) {
		classComment = comment;
		lastUpdatedTime = Instant.now();
	}

	@Nullable
	@Override
	public String getFieldComment(@Nonnull String name, @Nonnull String descriptor) {
		return fieldComments.get(name + ' ' + descriptor);
	}

	@Nullable
	@Override
	public String getMethodComment(@Nonnull String name, @Nonnull String descriptor) {
		return methodComments.get(name + descriptor);
	}

	@Override
	public void setFieldComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment) {
		String key = name + ' ' + descriptor;
		if (comment == null)
			fieldComments.remove(key);
		else
			fieldComments.put(key, comment);
		lastUpdatedTime = Instant.now();
	}

	@Override
	public void setMethodComment(@Nonnull String name, @Nonnull String descriptor, @Nullable String comment) {
		String key = name + descriptor;
		if (comment == null)
			methodComments.remove(key);
		else
			methodComments.put(key, comment);
		lastUpdatedTime = Instant.now();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PersistClassComments that = (PersistClassComments) o;
		if (!fieldComments.equals(that.fieldComments)) return false;
		if (!methodComments.equals(that.methodComments)) return false;
		return Objects.equals(classComment, that.classComment);
	}

	@Override
	public int hashCode() {
		int result = fieldComments.hashCode();
		result = 31 * result + methodComments.hashCode();
		result = 31 * result + (classComment != null ? classComment.hashCode() : 0);
		return result;
	}
}
