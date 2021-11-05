package me.coley.recaf.util;

/**
 * Number parsing and comparison.
 *
 * @author Matt Coley
 */
public class NumberUtil {
	/**
	 * @param input
	 * 		Text input that represents a number.
	 * 		<ul>
	 * 		<li>Numbers ending with {@code F} are parsed as {@link Float}.</li>
	 * 		<li>Numbers ending with {@code D} are parsed as {@link Double}.</li>
	 * 		<li>Numbers ending with {@code L} are parsed as {@link Long}.</li>
	 * 		</ul>
	 *
	 * @return Number parsed from text.
	 * Can be an {@link Integer}, {@link Long}, {@link Float}, or {@link Double}.
	 */
	@SuppressWarnings("")
	public static Number parse(String input) {
		String text = input.trim().toUpperCase();
		Number value;
		if (text.indexOf('.') > 0) {
			value = parseDecimal(text);
		} else {
			if (text.endsWith("L"))
				value = Long.parseLong(text.substring(0, text.indexOf("L")));
			else if (text.startsWith("0X"))
				value = Integer.parseInt(text.substring(2), 16);
			else if (text.endsWith("F"))
				value = Float.parseFloat(text.substring(0, text.indexOf("F")));
			else if (text.endsWith("D") || text.contains("."))
				value = Double.parseDouble(text.substring(0, text.indexOf("D")));
			else
				value = Integer.parseInt(text);
		}
		return value;
	}

	/**
	 * @param text
	 * 		Text input that represents a decimal number.
	 *
	 * @return Decimal number, either {@link Float} or {@link Double}.
	 */
	private static Number parseDecimal(String text) {
		if (text.endsWith("F"))
			return Float.parseFloat(text.substring(0, text.indexOf("F")));
		else if (text.endsWith("D") || text.contains("."))
			return Double.parseDouble(text.substring(0, text.indexOf("D")));
		else
			return Double.parseDouble(text);
	}

	/**
	 * Compare two numeric values, regardless of their type.
	 *
	 * @param right
	 * 		First value.
	 * @param left
	 * 		Second value.
	 *
	 * @return Comparison of {@code X.compare(left, right)}
	 */
	public static int cmp(Number left, Number right) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (right instanceof Double || left instanceof Double) {
			return Double.compare(left.doubleValue(), right.doubleValue());
		} else if (right instanceof Float || left instanceof Float) {
			return Float.compare(left.floatValue(), right.floatValue());
		} else if (right instanceof Long || left instanceof Long) {
			return Long.compare(left.longValue(), right.longValue());
		} else {
			return Integer.compare(left.intValue(), right.intValue());
		}
	}
}
