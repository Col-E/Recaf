package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Matcher outline for comparing one string to multiple strings.
 *
 * @author Matt Coley
 */
public interface MultiStringMatcher extends BiMatcher<Collection<String>, String> {
	@Override
	boolean matches(@Nonnull Collection<String> keys, @Nonnull String target);
}
