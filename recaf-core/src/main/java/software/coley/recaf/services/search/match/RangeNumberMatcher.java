package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matcher outline for comparing one number to a range of numbers.
 *
 * @author Matt Coley
 */
public interface RangeNumberMatcher {
	/**
	 * @param lower
	 * 		Lower target value range to match against.
	 * @param upper
	 * 		Upper target value range to match against.
	 * @param target
	 * 		Value to check.
	 *
	 * @return {@code true} when the target value matches the given range.
	 */
	boolean test(@Nonnull Number lower, @Nonnull Number upper, @Nonnull Number target);
}
