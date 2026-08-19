package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;

/**
 * Constraint over the number of values consumed by a list matcher.
 *
 * @param count
 * 		Target count.
 * @param arity
 * 		Count comparison mode.
 *
 * @author Matt Coley
 * @see ListMatcher
 */
public record CountConstraint(int count, @Nonnull Arity arity) {
	/**
	 * @param count
	 * 		Target count.
	 * @param arity
	 * 		Count comparison mode.
	 *
	 * @throws IllegalArgumentException
	 * 		When {@code count} is negative.
	 */
	public CountConstraint {
		if (count < 0)
			throw new IllegalArgumentException("Count cannot be negative");
	}

	/**
	 * @param actual
	 * 		Consumed value count.
	 *
	 * @return {@code true} when the count satisfies this constraint.
	 */
	public boolean matches(int actual) {
		return switch (arity) {
			case EXACT -> actual == count;
			case AT_LEAST -> actual >= count;
			case AT_MOST -> actual <= count;
		};
	}

	/**
	 * @param count
	 * 		Exact count.
	 *
	 * @return Exact constraint.
	 */
	@Nonnull
	public static CountConstraint exact(int count) {
		return new CountConstraint(count, Arity.EXACT);
	}

	/**
	 * @param count
	 * 		Minimum count.
	 *
	 * @return At-least constraint.
	 */
	@Nonnull
	public static CountConstraint atLeast(int count) {
		return new CountConstraint(count, Arity.AT_LEAST);
	}

	/**
	 * @param count
	 * 		Maximum count.
	 *
	 * @return At-most constraint.
	 */
	@Nonnull
	public static CountConstraint atMost(int count) {
		return new CountConstraint(count, Arity.AT_MOST);
	}
}
