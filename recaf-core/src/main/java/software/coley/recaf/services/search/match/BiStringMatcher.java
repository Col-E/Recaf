package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matcher outline for comparing one string to another.
 *
 * @author Matt Coley
 */
public interface BiStringMatcher extends BiMatcher<String, String> {
	@Override
	boolean matches(@Nonnull String key, @Nonnull String target);
}
