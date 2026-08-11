package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matcher outline for comparing one value to another.
 * <p>
 * Generally, {@code K} will be the same as {@code V}, but this is not required as some cases
 * may want the {@code K} to be a collection of value types.
 *
 * @param <K>
 * 		Key type.
 * @param <V>
 * 		Value type.
 *
 * @author Matt Coley
 * @see Matcher
 */
@FunctionalInterface
public interface BiMatcher<K, V> {
	/**
	 * @param key
	 * 		Target value to match against.
	 * @param target
	 * 		Value to check.
	 *
	 * @return {@code true} when the target value matches the key value.
	 */
	boolean matches(@Nonnull K key, @Nonnull V target);
}
