package software.coley.recaf.services.search.match;

import jakarta.annotation.Nullable;

/**
 * Matches one possibly {@code null} value.
 *
 * @param <T>
 * 		Value type.
 *
 * @author Matt Coley
 * @see BiMatcher
 */
@FunctionalInterface
public interface Matcher<T> {
	/**
	 * @param value
	 * 		Value to test.
	 *
	 * @return {@code true} when the value matches.
	 */
	boolean matches(@Nullable T value);
}
