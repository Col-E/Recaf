package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.match.AnyMatcher;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.query.structure.android.AnyDexInsnMatcher;
import software.coley.recaf.services.search.query.structure.jvm.AnyJvmInsnMatcher;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Common utilities for matching structure queries against candidate structures.
 *
 * @author Matt Coley
 */
public class StructMatchUtils {
	/**
	 * Matches candidate values using the configured order and count rules.
	 *
	 * @param specification
	 * 		List matching specification.
	 * @param values
	 * 		Candidate values.
	 * @param predicate
	 * 		Matcher predicate.
	 * @param <T>
	 * 		Matcher type.
	 * @param <V>
	 * 		Candidate value type.
	 *
	 * @return {@code true} when the specification matches.
	 */
	public static <T, V> boolean matchList(@Nonnull ListMatcher<T> specification,
	                                       @Nonnull List<V> values,
	                                       @Nonnull BiPredicate<T, V> predicate) {
		// Empty specifications only match empty candidates.
		if (specification.matchers().isEmpty())
			return values.isEmpty() && specification.count().matches(0);

		// Use distinct assignments for bags and ordered scanning for sequences.
		return specification.order() == OrderMode.BAG ?
				matchBag(specification, values, predicate, 0, new boolean[values.size()], 0) :
				matchSequence(specification, values, predicate, 0, 0, 0);
	}

	/**
	 * Recursively assigns each bag matcher to a distinct candidate.
	 *
	 * @param specification
	 * 		List matching specification.
	 * @param values
	 * 		Candidate values.
	 * @param predicate
	 * 		Matcher predicate.
	 * @param matcherIndex
	 * 		Current matcher index.
	 * @param used
	 * 		Assignment markers.
	 * @param consumed
	 * 		Number of assigned values.
	 * @param <T>
	 * 		Matcher type.
	 * @param <V>
	 * 		Candidate value type.
	 *
	 * @return {@code true} when a complete assignment satisfies the count.
	 */
	public static <T, V> boolean matchBag(@Nonnull ListMatcher<T> specification,
	                                      @Nonnull List<V> values,
	                                      @Nonnull BiPredicate<T, V> predicate,
	                                      int matcherIndex, boolean[] used, int consumed) {
		// Stop once every matcher has an assignment.
		if (matcherIndex == specification.matchers().size())
			return specification.count().matches(consumed);

		// Try each candidate value for the current matcher, skipping already assigned values.
		T matcher = specification.matchers().get(matcherIndex);
		for (int index = 0; index < values.size(); index++) {
			if (used[index] || !matches(matcher, values.get(index), predicate))
				continue;
			used[index] = true;
			if (matchBag(specification, values, predicate, matcherIndex + 1, used, consumed + 1))
				return true;
			used[index] = false;
		}

		// No unused candidate satisfied the current matcher.
		return false;
	}

	/**
	 * Recursively scans candidates in sequence order.
	 *
	 * @param specification
	 * 		List matching specification.
	 * @param values
	 * 		Candidate values.
	 * @param predicate
	 * 		Matcher predicate.
	 * @param matcherIndex
	 * 		Current matcher index.
	 * @param valueIndex
	 * 		Current candidate index.
	 * @param consumed
	 * 		Number of consumed values.
	 * @param <T>
	 * 		Matcher type.
	 * @param <V>
	 * 		Candidate value type.
	 *
	 * @return {@code true} when the remaining sequence matches.
	 */
	public static <T, V> boolean matchSequence(@Nonnull ListMatcher<T> specification,
	                                           @Nonnull List<V> values,
	                                           @Nonnull BiPredicate<T, V> predicate,
	                                           int matcherIndex, int valueIndex, int consumed) {
		// Stop once every matcher has consumed the remaining candidate suffix.
		if (matcherIndex == specification.matchers().size())
			return valueIndex == values.size() && specification.count().matches(consumed);

		// Wildcards may either consume nothing or span one candidate.
		T matcher = specification.matchers().get(matcherIndex);
		if (matcher == InsnMatcher.ANY || matcher instanceof AnyMatcher<?> ||
				matcher instanceof AnyJvmInsnMatcher || matcher instanceof AnyDexInsnMatcher) {
			if (matchSequence(specification, values, predicate, matcherIndex + 1, valueIndex, consumed))
				return true;
			return valueIndex < values.size() &&
					matchSequence(specification, values, predicate, matcherIndex, valueIndex + 1, consumed + 1);
		}

		// Ordinary matchers consume exactly one candidate.
		return valueIndex < values.size()
				&& matches(matcher, values.get(valueIndex), predicate)
				&& matchSequence(specification, values, predicate, matcherIndex + 1, valueIndex + 1, consumed + 1);
	}

	/**
	 * Applies a matcher unless it is the generic wildcard.
	 *
	 * @param matcher
	 * 		Matcher to apply.
	 * @param value
	 * 		Candidate value.
	 * @param predicate
	 * 		Matcher predicate.
	 * @param <T>
	 * 		Matcher type.
	 * @param <V>
	 * 		Candidate value type.
	 *
	 * @return {@code true} when the matcher accepts the value.
	 */
	public static <T, V> boolean matches(@Nonnull T matcher,
	                                     @Nonnull V value,
	                                     @Nonnull BiPredicate<T, V> predicate) {
		return matcher instanceof AnyMatcher<?> || predicate.test(matcher, value);
	}

	/**
	 * Matches a JVM instruction opcode.
	 *
	 * @param node
	 * 		Instruction node.
	 * @param matcher
	 * 		Opcode matcher.
	 *
	 * @return {@code true} when the opcode matches.
	 */
	public static boolean opcodeJvm(@Nonnull AbstractInsnNode node, @Nonnull Matcher<Integer> matcher) {
		return matcher.matches(node.getOpcode());
	}
}
