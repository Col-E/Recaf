package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

/**
 * Matcher backed by a predicate.
 *
 * @param <T>
 * 		Value type.
 * @param predicate
 * 		Predicate to evaluate.
 *
 * @author Matt Coley
 */
public record PredicateMatcher<T>(@Nonnull Predicate<? super T> predicate) implements Matcher<T> {
	@Override
	public boolean matches(@Nullable T value) {
		return predicate.test(value);
	}
}
