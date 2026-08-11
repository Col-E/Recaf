package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matcher outline for comparing one number to another.
 *
 * @author Matt Coley
 */
public interface BiNumberMatcher extends BiMatcher<Number, Number> {
	@Override
	boolean matches(@Nonnull Number key, @Nonnull Number target);
}
