package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

import java.util.Objects;

/**
 * The base result contains path information of the matched value.
 *
 * @author Matt Coley
 */
public abstract class Result<T> implements Comparable<Result<?>> {
	private final PathNode<?> path;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 */
	public Result(@Nonnull PathNode<?> path) {
		this.path = path;
	}

	/**
	 * @return Wrapped value, used internally for {@link #toString()}.
	 */
	@Nonnull
	protected abstract T getValue();

	/**
	 * @return Path to item containing the result.
	 */
	@Nonnull
	public PathNode<?> getPath() {
		return path;
	}

	@Override
	public int compareTo(@Nonnull Result<?> o) {
		if (o == this)
			return 0;

		// Base comparison by path.
		int cmp = path.compareTo(o.path);

		// Disambiguate if path is the same, but values differ.
		if (cmp == 0)
			cmp = Integer.compare(getValue().hashCode(), o.getValue().hashCode());

		return cmp;
	}

	@Override
	public String toString() {
		return "Result{value=" + getValue() + ", path=" + path + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result<?> result = (Result<?>) o;
		return Objects.equals(path, result.path) &&
				Objects.equals(getValue(), result.getValue());
	}

	@Override
	public int hashCode() {
		int result = path.hashCode();
		result = 31 * result + Objects.hashCode(getValue());
		return result;
	}
}