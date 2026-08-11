package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;

import java.util.function.Predicate;

/**
 * Factories for the common matcher implementations.
 *
 * @author Matt Coley
 * @see Matcher
 */
public final class Matchers {
	/**
	 * Prevents construction of this utility class.
	 */
	private Matchers() {}

	/**
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Shared wildcard matcher.
	 */
	public static <T> Matcher<T> any() {
		return AnyMatcher.instance();
	}

	/**
	 * @param regex
	 * 		Regular expression.
	 *
	 * @return Regular-expression matcher.
	 */
	@Nonnull
	public static Matcher<String> regex(@Nonnull String regex) {
		return new RegexMatcher(regex);
	}

	/**
	 * @param text
	 * 		Text to check for containment.
	 *
	 * @return Containing string matcher.
	 */
	@Nonnull
	public static Matcher<String> contains(@Nonnull String text) {
		return value -> value != null && value.contains(text);
	}

	/**
	 * @param text
	 * 		Text to check for prefix.
	 *
	 * @return Prefix string matcher.
	 */
	@Nonnull
	public static Matcher<String> startsWith(@Nonnull String text) {
		return value -> value != null && value.startsWith(text);
	}

	/**
	 * @param text
	 * 		Text to check for suffix.
	 *
	 * @return Suffix string matcher.
	 */
	@Nonnull
	public static Matcher<String> endsWith(@Nonnull String text) {
		return value -> value != null && value.endsWith(text);
	}

	/**
	 * @param type
	 * 		Expected ASM type.
	 *
	 * @return Exact ASM type matcher.
	 */
	@Nonnull
	public static Matcher<Type> type(@Nullable Type type) {
		return exact(type);
	}

	/**
	 * @param value
	 * 		Expected value.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Exact matcher.
	 */
	@Nonnull
	public static <T> Matcher<T> exact(@Nullable T value) {
		return new ExactMatcher<>(value);
	}

	/**
	 * @param predicate
	 * 		Existing string predicate.
	 *
	 * @return Adapter preserving the existing predicate behavior.
	 */
	@Nonnull
	public static Matcher<String> stringPredicate(@Nonnull StringPredicate predicate) {
		return predicate(predicate::match);
	}

	/**
	 * @param predicate
	 * 		Predicate to invoke.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Predicate matcher.
	 */
	@Nonnull
	public static <T> Matcher<T> predicate(@Nonnull Predicate<? super T> predicate) {
		return new PredicateMatcher<>(predicate);
	}

	/**
	 * @param predicate
	 * 		Existing numeric predicate.
	 *
	 * @return Adapter preserving the existing predicate behavior.
	 */
	@Nonnull
	public static Matcher<Number> numberPredicate(@Nonnull NumberPredicate predicate) {
		return predicate(predicate::match);
	}
}
