package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * Matcher implementations for string values.
 *
 * @author Matt Coley
 */
public class StringPredicate {
	/** Translation key prefix */
	public static String TRANSLATION_PREFIX = "string.match.";
	private final Predicate<String> delegate;
	private final String id;

	/**
	 * @param id
	 * 		Predicate ID.
	 * @param delegate
	 * 		Matcher predicate implementation.
	 */
	public StringPredicate(@Nonnull String id, @Nonnull Predicate<String> delegate) {
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
	 * @param text
	 * 		Text to test for a match.
	 *
	 * @return {@code true} if the given string matches with a given key value.
	 */
	public boolean match(@Nonnull String text) {
		return delegate.test(text);
	}
}