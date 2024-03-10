package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.util.RegexUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider of {@link StringPredicate} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class StringPredicateProvider {
	private final Map<String, BiStringMatcher> biStringMatchers = new ConcurrentHashMap<>();
	private final Map<String, MultiStringMatcher> multiStringMatchers = new ConcurrentHashMap<>();

	@Inject
	public StringPredicateProvider() {
		registerBiMatcher("equal", String::equals);
		registerBiMatcher("equal-ic", String::equalsIgnoreCase);
		registerBiMatcher("contains", (key, value) -> value.contains(key));
		registerBiMatcher("contains-ic", (key, value) -> value.toLowerCase().contains(key.toLowerCase()));
		registerBiMatcher("starts", (key, value) -> value.startsWith(key));
		registerBiMatcher("starts-ic", (key, value) -> value.toLowerCase().startsWith(key.toLowerCase()));
		registerBiMatcher("ends", (key, value) -> value.endsWith(key));
		registerBiMatcher("ends-ic", (key, value) -> value.toLowerCase().endsWith(key.toLowerCase()));
		registerBiMatcher("regex-partial", (key, value) -> {
			try {
				return RegexUtil.getMatcher(key, value).find();
			} catch (Throwable t) {
				// Invalid regex pattern, logged by regex-util
				return false;
			}
		});
		registerBiMatcher("regex-full", (key, value) -> {
			try {
				return RegexUtil.getMatcher(key, value).matches();
			} catch (Throwable t) {
				// Invalid regex pattern, logged by regex-util
				return false;
			}
		});
	}

	/**
	 * @param id
	 * 		Unique ID to register with.
	 * @param matcher
	 * 		Matcher implementation.
	 *
	 * @return {@code true} on success. {@code false} if the ID is already in-use.
	 */
	public boolean registerBiMatcher(@Nonnull String id, @Nonnull BiStringMatcher matcher) {
		return biStringMatchers.putIfAbsent(id, matcher) == null;
	}

	/**
	 * @param id
	 * 		Unique ID to register with.
	 * @param matcher
	 * 		Matcher implementation.
	 *
	 * @return {@code true} on success. {@code false} if the ID is already in-use.
	 */
	public boolean registerMultiMatcher(@Nonnull String id, @Nonnull MultiStringMatcher matcher) {
		return multiStringMatchers.putIfAbsent(id, matcher) == null;
	}

	/**
	 * @param key
	 * 		String to match against, case-sensitive.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newEqualPredicate(@Nonnull String key) {
		return newEqualPredicate(key, true);
	}

	/**
	 * @param key
	 * 		String to match against.
	 * @param caseSensitive
	 * 		Whether the match should be case-sensitive or not.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newEqualPredicate(@Nonnull String key, boolean caseSensitive) {
		return Objects.requireNonNull(newBiStringPredicate(caseSensitive ? "equal" : "equal-ic", key));
	}

	/**
	 * @param key
	 * 		String to match against, case-sensitive.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newContainsPredicate(@Nonnull String key) {
		return newContainsPredicate(key, true);
	}

	/**
	 * @param key
	 * 		String to match against.
	 * @param caseSensitive
	 * 		Whether the match should be case-sensitive or not.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newContainsPredicate(@Nonnull String key, boolean caseSensitive) {
		return Objects.requireNonNull(newBiStringPredicate(caseSensitive ? "contains" : "contains-ic", key));
	}

	/**
	 * @param key
	 * 		String to match against, case-sensitive.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newStartsWithPredicate(@Nonnull String key) {
		return newStartsWithPredicate(key, true);
	}

	/**
	 * @param key
	 * 		String to match against.
	 * @param caseSensitive
	 * 		Whether the match should be case-sensitive or not.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newStartsWithPredicate(@Nonnull String key, boolean caseSensitive) {
		return Objects.requireNonNull(newBiStringPredicate(caseSensitive ? "starts" : "starts-ic", key));
	}

	/**
	 * @param key
	 * 		String to match against, case-sensitive.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newEndsWithPredicate(@Nonnull String key) {
		return newEndsWithPredicate(key, true);
	}

	/**
	 * @param key
	 * 		String to match against.
	 * @param caseSensitive
	 * 		Whether the match should be case-sensitive or not.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newEndsWithPredicate(@Nonnull String key, boolean caseSensitive) {
		return Objects.requireNonNull(newBiStringPredicate(caseSensitive ? "ends" : "ends-ic", key));
	}

	/**
	 * @param regex
	 * 		Pattern to match against. Only part of the target string needs to match.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newPartialRegexPredicate(@Nonnull String regex) {
		return Objects.requireNonNull(newBiStringPredicate("regex-partial", regex));
	}

	/**
	 * @param regex
	 * 		Pattern to match against. The entire target string needs to match.
	 *
	 * @return Predicate to target the given string.
	 */
	@Nonnull
	public StringPredicate newFullRegexPredicate(@Nonnull String regex) {
		return Objects.requireNonNull(newBiStringPredicate("regex-full", regex));
	}

	/**
	 * @param id
	 * 		Matcher unique ID.
	 * @param key
	 * 		String to match against.
	 *
	 * @return Predicate to target the given string.
	 *
	 * @throws NoSuchElementException
	 * 		When no matcher implementation is registered with the given ID.
	 */
	@Nullable
	public StringPredicate newBiStringPredicate(@Nonnull String id, @Nonnull String key) throws NoSuchElementException {
		BiStringMatcher matcher = biStringMatchers.get(id);
		if (matcher != null)
			return new StringPredicate(id, target -> matcher.test(key, target));
		throw new NoSuchElementException("No such single-parameter matcher: " + id);
	}

	/**
	 * @param id
	 * 		Matcher unique ID.
	 * @param keys
	 * 		Collection of strings to match against.
	 *
	 * @return Predicate to target the given strings.
	 *
	 * @throws NoSuchElementException
	 * 		When no matcher implementation is registered with the given ID.
	 */
	@Nullable
	public StringPredicate newMultiStringPredicate(@Nonnull String id, @Nonnull Collection<String> keys) throws NoSuchElementException {
		MultiStringMatcher matcher = multiStringMatchers.get(id);
		if (matcher != null)
			return new StringPredicate(id, target -> matcher.test(keys, target));
		throw new NoSuchElementException("No such multi-parameter matcher: " + id);
	}

	/**
	 * @return Map of matcher keys to implementations.
	 */
	@Nonnull
	public Map<String, BiStringMatcher> getBiStringMatchers() {
		return Collections.unmodifiableMap(biStringMatchers);
	}

	/**
	 * @return Map of matcher keys to implementations.
	 */
	@Nonnull
	public Map<String, MultiStringMatcher> getMultiStringMatchers() {
		return Collections.unmodifiableMap(multiStringMatchers);
	}
}
