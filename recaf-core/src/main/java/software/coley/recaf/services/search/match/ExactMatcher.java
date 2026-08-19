package software.coley.recaf.services.search.match;

import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Matcher using {@link Objects#equals(Object, Object)}.
 *
 * @param <T>
 * 		Value type.
 * @param expected
 * 		Expected value.
 *
 * @author Matt Coley
 */
public record ExactMatcher<T>(@Nullable T expected) implements Matcher<T> {
	@Override
	public boolean matches(@Nullable T value) {
		return Objects.equals(expected, value);
	}
}
