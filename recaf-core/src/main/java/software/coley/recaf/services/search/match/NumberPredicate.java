package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * Matcher implementations for numeric values.
 *
 * @author Matt Coley
 */
public class NumberPredicate {
	/** Translation key prefix */
	public static final String TRANSLATION_PREFIX = "number.match.";
	private final Predicate<Number> delegate;
	private final String id;

	/**
	 * @param id
	 * 		Predicate ID.
	 * @param delegate
	 * 		Matcher predicate implementation.
	 */
	public NumberPredicate(@Nonnull String id, @Nonnull Predicate<Number> delegate) {
		this.delegate = delegate;
		this.id = id;
	}

	/**
	 * @return Predicate ID.
	 */
	@Nonnull
	public String getId() {
		return id;
	}

	/**
	 * @return Translation key for predicate.
	 */
	@Nonnull
	public String getTranslationKey() {
		return TRANSLATION_PREFIX + getId();
	}

	/**
	 * @param value
	 * 		Value to test for a match.
	 *
	 * @return {@code true} if the given value matches.
	 */
	public boolean match(@Nonnull Number value) {
		return delegate.test(value);
	}
}