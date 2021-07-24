package me.coley.recaf.search;

import java.util.function.BiPredicate;

/**
 * Match implementations for numeric values.
 *
 * @author Matt coley
 */
public enum NumberMatchMode {
	/**
	 * Value match via equality.
	 */
	EQUALS((key, text) -> text.equals(key)),
	/**
	 * Value match via {@code k > v}.
	 */
	GREATER_THAN((key, text) -> cmp(key, text) > 0),
	/**
	 * Value match via {@code k >= v}.
	 */
	GREATER_OR_EQUAL_THAN((key, text) -> cmp(key, text) >= 0),
	/**
	 * Value match via {@code k < v}.
	 */
	LESS_THAN((key, text) -> cmp(key, text) < 0),
	/**
	 * Value match via {@code k <= v}.
	 */
	LESS_OR_EQUAL_THAN((key, text) -> cmp(key, text) <= 0);

	private final BiPredicate<Number, Number> matcher;

	NumberMatchMode(BiPredicate<Number, Number> matcher) {
		this.matcher = matcher;
	}

	/**
	 * @param key
	 * 		Expected pattern.
	 * @param text
	 * 		Value to test for a match.
	 *
	 * @return {@code true} if the given value matches with the given key.
	 */
	public boolean match(Number key, Number text) {
		return matcher.test(key, text);
	}

	private static int cmp(Number key, Number value) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (key instanceof Double || value instanceof Double) {
			return Double.compare(value.doubleValue(), key.doubleValue());
		} else if (key instanceof Float || value instanceof Float) {
			return Float.compare(value.floatValue(), key.floatValue());
		} else if (key instanceof Long || value instanceof Long) {
			return Long.compare(value.longValue(), key.longValue());
		} else {
			return Integer.compare(value.intValue(), key.intValue());
		}
	}
}