package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Matcher outline for comparing one string to multiple strings.
 *
 * @author Matt Coley
 */
public interface MultiStringMatcher {
	/**
	 * @param keys
	 * 		Target values to match against.
	 * @param target
	 * 		Value to check.
	 *
	 * @return {@code true} when the target value matches the key value(s).
	 */
	boolean test(@Nonnull Collection<String> keys, @Nonnull String target);
}
