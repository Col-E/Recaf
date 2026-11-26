package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Matcher outline for comparing one number to multiple numbers.
 *
 * @author Matt Coley
 */
public interface MultiNumberMatcher extends BiMatcher<Collection<Number>, Number> {
	@Override
	boolean matches(@Nonnull Collection<Number> keys, @Nonnull Number target);
}
