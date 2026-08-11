package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Matcher accepting every value.
 *
 * @param <T>
 * 		Value type.
 *
 * @author Matt Coley
 */
public final class AnyMatcher<T> implements Matcher<T> {
	private static final AnyMatcher<?> INSTANCE = new AnyMatcher<>();

	private AnyMatcher() {}

	/**
	 * Accepts every value, including {@code null}.
	 *
	 * @param value
	 * 		Value to test.
	 *
	 * @return Always {@code true}.
	 */
	@Override
	public boolean matches(@Nullable T value) {
		return true;
	}

	/**
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Shared wildcard matcher.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> AnyMatcher<T> instance() {
		return (AnyMatcher<T>) INSTANCE;
	}
}
