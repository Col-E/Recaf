package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Matcher outline for comparing one number to multiple numbers.
 *
 * @author Matt Coley
 */
public interface MultiNumberMatcher {
	/**
	 * @param keys
	 * 		Target values to match against.
	 * @param target
	 * 		Value to check.
	 *
	 * @return {@code true} when the target value matches the key value(s).
	 */
	boolean test(@Nonnull Collection<Number> keys, @Nonnull Number target);
}
