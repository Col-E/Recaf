package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matcher outline for comparing one number to another.
 *
 * @author Matt Coley
 */
public interface BiNumberMatcher {
	/**
	 * @param key
	 * 		Target value to match against.
	 * @param target
	 * 		Value to check.
	 *
	 * @return {@code true} when the target value matches the key value.
	 */
	boolean test(@Nonnull Number key, @Nonnull Number target);
}
