package me.coley.recaf.search.result;

import java.util.Objects;

/**
 * The base result contains location information of the matched value.
 *
 * @author Matt Coley
 */
public abstract class Result implements Comparable<Result> {
	private final Location location;

	/**
	 * @param builder
	 * 		Builder containing information about the result.
	 */
	public Result(ResultBuilder builder) {
		if (builder.getContainingClass() != null) {
			this.location = new ClassLocation(builder);
		} else {
			this.location = new FileLocation(builder);
		}
	}

	/**
	 * @return Wrapped value, used internally for {@link #toString()}.
	 */
	protected abstract Object getValue();

	/**
	 * @return Location of result.
	 */
	public Location getLocation() {
		return location;
	}

	@Override
	public int compareTo(Result o) {
		if (o == this) {
			return 0;
		}
		return location.compareTo(o.location);
	}

	@Override
	public String toString() {
		return "Result{value=" + getValue() + ", Location=" + location + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result result = (Result) o;
		return Objects.equals(location, result.location);
	}

	@Override
	public int hashCode() {
		return location.hashCode();
	}
}
