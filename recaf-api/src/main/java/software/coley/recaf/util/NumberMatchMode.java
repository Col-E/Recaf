package software.coley.recaf.util;

import java.util.function.BiPredicate;

import static software.coley.recaf.util.NumberUtil.cmp;

/**
 * Match implementations for numeric values.
 *
 * @author Matt Coley
 */
public enum NumberMatchMode {
	/**
	 * Value match via equality.
	 */
	EQUALS((key, value) -> cmp(key, value) == 0),
	/**
	 * Value match via not equality.
	 */
	NOT((key, value) -> cmp(key, value) != 0),
	/**
	 * Value match via {@code k > v}.
	 */
	GREATER_THAN((key, value) -> cmp(key, value) < 0),
	/**
	 * Value match via {@code k >= v}.
	 */
	GREATER_OR_EQUAL_THAN((key, value) -> cmp(key, value) <= 0),
	/**
	 * Value match via {@code k < v}.
	 */
	LESS_THAN((key, value) -> cmp(key, value) > 0),
	/**
	 * Value match via {@code k <= v}.
	 */
	LESS_OR_EQUAL_THAN((key, text) -> cmp(key, text) >= 0);

	private final BiPredicate<Number, Number> matcher;

	NumberMatchMode(BiPredicate<Number, Number> matcher) {
		this.matcher = matcher;
	}

	/**
	 * @param key
	 * 		Expected pattern.
	 * @param value
	 * 		Value to test for a match.
	 *
	 * @return {@code true} if the given value matches with the given key.
	 */
	public boolean match(Number key, Number value) {
		return matcher.test(key, value);
	}
}