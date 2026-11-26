package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import software.coley.collections.Unchecked;
import software.coley.recaf.services.search.match.AnyMatcher;
import software.coley.recaf.services.search.match.Matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Matches a collection of values using distinct assignments or an ordered scan.
 *
 * @param matchers
 * 		Matchers to apply.
 * @param count
 * 		Count constraint over the consumed candidates.
 * @param order
 * 		Assignment order.
 * @param <T>
 * 		Matcher element type.
 *
 * @author Matt Coley
 * @see CountConstraint
 * @see OrderMode
 */
public record ListMatcher<T>(@Nonnull List<T> matchers,
                             @Nonnull CountConstraint count, @Nonnull OrderMode order) {
	private static final ListMatcher<?> ANY = new ListMatcher<>(List.of(AnyMatcher.instance()), CountConstraint.atLeast(0), OrderMode.SEQUENCE);

	/**
	 * @param values
	 * 		Candidate values.
	 *
	 * @return {@code true} when this list constraint matches.
	 */
	public boolean matches(@Nonnull List<?> values) {
		// Empty specifications only match empty candidates.
		if (matchers.isEmpty())
			return values.isEmpty() && count.matches(0);

		return order == OrderMode.BAG ? matchesBag(values) : matchesSequence(values);
	}

	/**
	 * Starts unordered matching with a fresh assignment map.
	 *
	 * @param values
	 * 		Candidate values.
	 *
	 * @return {@code true} when the bag matches.
	 */
	private boolean matchesBag(@Nonnull List<?> values) {
		boolean[] used = new boolean[values.size()];
		return matchBag(0, values, used, 0);
	}

	/**
	 * Assigns each matcher to a distinct candidate value.
	 *
	 * @param matcherIndex
	 * 		Current matcher index.
	 * @param values
	 * 		Candidate values.
	 * @param used
	 * 		Assignment markers.
	 * @param consumed
	 * 		Number of assigned values.
	 *
	 * @return {@code true} when a complete assignment satisfies the count.
	 */
	private boolean matchBag(int matcherIndex,
	                         @Nonnull List<?> values,
	                         boolean[] used, int consumed) {
		// Stop once every matcher has an assignment.
		if (matcherIndex == matchers.size())
			return count.matches(consumed);

		// Try each candidate value for the current matcher, skipping already assigned values.
		Object matcher = matchers.get(matcherIndex);
		for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
			if (used[valueIndex] || !matchesValue(matcher, values.get(valueIndex)))
				continue;
			used[valueIndex] = true;
			if (matchBag(matcherIndex + 1, values, used, consumed + 1))
				return true;
			used[valueIndex] = false;
		}

		// No unused candidate values matched the current matcher.
		return false;
	}

	/**
	 * Starts ordered matching at the beginning of both lists.
	 *
	 * @param values
	 * 		Candidate values.
	 *
	 * @return {@code true} when the sequence matches.
	 */
	private boolean matchesSequence(@Nonnull List<?> values) {
		return matchSequence(0, 0, values, 0);
	}

	/**
	 * Scans the candidate list while allowing wildcards to consume spans.
	 *
	 * @param matcherIndex
	 * 		Current matcher index.
	 * @param valueIndex
	 * 		Current candidate index.
	 * @param values
	 * 		Candidate values.
	 * @param consumed
	 * 		Number of consumed values.
	 *
	 * @return {@code true} when the remaining sequence matches.
	 */
	private boolean matchSequence(int matcherIndex, int valueIndex, @Nonnull List<?> values, int consumed) {
		// If all matchers have been applied, the candidate list must also be fully consumed and satisfy the count.
		// If there are still matchers left, the candidate list must have more values to consume.
		if (matcherIndex == matchers.size())
			return valueIndex == values.size() && count.matches(consumed);

		// If there are still matchers left, the candidate list must have more values to consume.
		Object matcher = matchers.get(matcherIndex);
		if (isWildcard(matcher)) {
			// Try the zero-width choice first so wildcard spans are deterministic.
			if (matchSequence(matcherIndex + 1, valueIndex, values, consumed))
				return true;

			// Try the one-or-more choice.
			return valueIndex < values.size() &&
					matchSequence(matcherIndex, valueIndex + 1, values, consumed + 1);
		}

		// If the matcher is not a wildcard, the candidate list must
		// have more values to consume and the next value must match.
		if (valueIndex >= values.size() || !matchesValue(matcher, values.get(valueIndex)))
			return false;

		// If the matcher is not a wildcard, the candidate list must
		// have more values to consume and the next value must match.
		return matchSequence(matcherIndex + 1, valueIndex + 1, values, consumed + 1);
	}

	/**
	 * Applies a typed matcher to one candidate value.
	 *
	 * @param matcher
	 * 		Matcher to apply.
	 * @param value
	 * 		Candidate value.
	 *
	 * @return {@code true} when the matcher accepts the value.
	 */
	@SuppressWarnings("unchecked")
	private static boolean matchesValue(@Nonnull Object matcher, @Nonnull Object value) {
		return matcher instanceof Matcher<?> typed && ((Matcher<Object>) typed).matches(value);
	}

	/**
	 * Identifies the wildcard implementation used by sequence matching.
	 *
	 * @param matcher
	 * 		Matcher to inspect.
	 *
	 * @return {@code true} when the matcher accepts an arbitrary span.
	 */
	private static boolean isWildcard(@Nonnull Object matcher) {
		return matcher instanceof AnyMatcher<?>;
	}

	/**
	 * @param <T>
	 * 		Item type of the list.
	 *
	 * @return Matcher that accepts any list of values.
	 */
	@Nonnull
	public static <T> ListMatcher<T> any() {
		return Unchecked.cast(ANY);
	}

	/**
	 * @param size
	 * 		Size of the list to match.
	 * @param <T>
	 * 		Item type of the list.
	 *
	 * @return Matcher that accepts any list of values of the given size.
	 */
	@Nonnull
	public static <T> ListMatcher<T> anyOfSize(int size) {
		return Unchecked.cast(new ListMatcher<>(List.of(AnyMatcher.instance()), CountConstraint.exact(size), OrderMode.SEQUENCE));
	}

	/**
	 * @param size
	 * 		Minimum size of the list to match.
	 * @param <T>
	 * 		Item type of the list.
	 *
	 * @return Matcher that accepts any list of values of at least the given size.
	 */
	@Nonnull
	public static <T> ListMatcher<T> anyOfSizeOrMore(int size) {
		return Unchecked.cast(new ListMatcher<>(List.of(AnyMatcher.instance()), CountConstraint.atLeast(size), OrderMode.SEQUENCE));
	}

	/**
	 * @param size
	 * 		Maximum size of the list to match.
	 * @param <T>
	 * 		Item type of the list.
	 *
	 * @return Matcher that accepts any list of values of at most the given size.
	 */
	@Nonnull
	public static <T> ListMatcher<T> anyOfSizeOrLess(int size) {
		return Unchecked.cast(new ListMatcher<>(List.of(AnyMatcher.instance()), CountConstraint.atMost(size), OrderMode.SEQUENCE));
	}

	/**
	 * @param matchers
	 * 		Matchers to apply in sequence.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Exact-size sequence matcher.
	 */
	@Nonnull
	public static <T> ListMatcher<T> sequence(@Nonnull List<T> matchers) {
		return sequence(matchers, CountConstraint.exact(matchers.size()));
	}

	/**
	 * @param matchers
	 * 		Matchers to apply in sequence.
	 * @param count
	 * 		Count constraint.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Sequence matcher.
	 */
	@Nonnull
	public static <T> ListMatcher<T> sequence(@Nonnull List<T> matchers, @Nonnull CountConstraint count) {
		return new ListMatcher<>(matchers, count, OrderMode.SEQUENCE);
	}

	/**
	 * @param matchers
	 * 		Matchers to apply in a bag.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Exact bag matcher.
	 */
	@SafeVarargs
	@Nonnull
	public static <T> ListMatcher<T> exactly(T... matchers) {
		List<T> values = new ArrayList<>(List.of(matchers));
		return bag(values);
	}

	/**
	 * @param matchers
	 * 		Matchers to apply as a bag.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Exact-size bag matcher.
	 */
	@Nonnull
	public static <T> ListMatcher<T> bag(@Nonnull List<T> matchers) {
		return bag(matchers, CountConstraint.exact(matchers.size()));
	}

	/**
	 * @param matchers
	 * 		Matchers to apply as a bag.
	 * @param count
	 * 		Count constraint.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Bag matcher.
	 */
	@Nonnull
	public static <T> ListMatcher<T> bag(@Nonnull List<T> matchers, @Nonnull CountConstraint count) {
		return new ListMatcher<>(matchers, count, OrderMode.BAG);
	}

	/**
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Matcher requiring an empty candidate list.
	 */
	@Nonnull
	public static <T> ListMatcher<T> empty() {
		return new ListMatcher<>(Collections.emptyList(), CountConstraint.exact(0), OrderMode.BAG);
	}

	/**
	 * @param matchers
	 * 		Matchers to apply as a bag.
	 * @param <T>
	 * 		Matcher element type.
	 *
	 * @return Exact bag matcher.
	 */
	@Nonnull
	public static <T> ListMatcher<T> exactly(@Nonnull List<T> matchers) {
		return bag(matchers);
	}
}
