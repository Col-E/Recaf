package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;

/**
 * General number parsing and other operations.
 *
 * @author Matt Coley
 */
public class NumberUtil {
	/**
	 * @param number
	 * 		Value to convert to string.
	 *
	 * @return String representation of value.
	 */
	@Nonnull
	public static String toString(@Nullable Number number) {
		if (number == null) return "0";

		if (number instanceof Integer || number instanceof Byte || number instanceof Short) {
			return Integer.toString(number.intValue());
		} else if (number instanceof Double) {
			return Double.toString(number.doubleValue());
		} else if (number instanceof Long) {
			return number.longValue() + "L";
		} else if (number instanceof Float) {
			return number.floatValue() + "F";
		}

		throw new IllegalArgumentException("Unsupported number type: " + number.getClass().getName());
	}

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
	 *
	 * @throws NumberFormatException
	 * 		When the input cannot be parsed.
	 */
	@SuppressWarnings("")
	@Nonnull
	public static Number parse(@Nonnull String input) {
		String text = input.trim().toUpperCase();
		Number value;
		if (text.indexOf('.') > 0) {
			value = parseDecimal(text);
		} else {
			if (text.endsWith("L") && text.startsWith("0X")) {
				String substring = text.substring(2, text.indexOf("L"));
				if (substring.isEmpty()) return 0L;
				value = Long.parseLong(substring, 16);
			} else if (text.endsWith("L"))
				value = Long.parseLong(text.substring(0, text.indexOf("L")));
			else if (text.startsWith("0X")) {
				String substring = text.substring(2);
				if (substring.isEmpty()) return 0;
				value = Integer.parseInt(substring, 16);
			} else if (text.endsWith("F"))
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
	@Nonnull
	private static Number parseDecimal(@Nonnull String text) {
		if (text.endsWith("F")) {
			int end = text.indexOf("F");
			if (end > 0)
				text = text.substring(0, end);
			return Float.parseFloat(text.substring(0, end));
		} else if (text.endsWith("D") || text.contains(".")) {
			int end = text.indexOf("D");
			if (end > 0)
				text = text.substring(0, end);
			return Double.parseDouble(text);
		} else
			return Double.parseDouble(text);
	}

	/**
	 * @param type1
	 * 		Numeric type. Must be a primitive.
	 * @param type2
	 * 		Numeric type. Must be a primitive.
	 *
	 * @return Widest primitive.
	 */
	@Nonnull
	public static Type getWidestType(@Nonnull Type type1, @Nonnull Type type2) {
		if (type1.getSort() > type2.getSort())
			return type1;
		return type2;
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
	public static int cmp(@Nonnull Number left, @Nonnull Number right) {
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

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Difference value.
	 */
	@Nonnull
	public static Number sub(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Double || first instanceof Double) {
			return first.doubleValue() - second.doubleValue();
		} else if (second instanceof Float || first instanceof Float) {
			return first.floatValue() - second.floatValue();
		} else if (second instanceof Long || first instanceof Long) {
			return first.longValue() - second.longValue();
		} else {
			return first.intValue() - second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Sum value.
	 */
	@Nonnull
	public static Number add(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Double || first instanceof Double) {
			return first.doubleValue() + second.doubleValue();
		} else if (second instanceof Float || first instanceof Float) {
			return first.floatValue() + second.floatValue();
		} else if (second instanceof Long || first instanceof Long) {
			return first.longValue() + second.longValue();
		} else {
			return first.intValue() + second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Product value.
	 */
	@Nonnull
	public static Number mul(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Double || first instanceof Double) {
			return first.doubleValue() * second.doubleValue();
		} else if (second instanceof Float || first instanceof Float) {
			return first.floatValue() * second.floatValue();
		} else if (second instanceof Long || first instanceof Long) {
			return first.longValue() * second.longValue();
		} else {
			return first.intValue() * second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Divided value.
	 */
	@Nonnull
	public static Number div(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Double || first instanceof Double) {
			return first.doubleValue() / second.doubleValue();
		} else if (second instanceof Float || first instanceof Float) {
			return first.floatValue() / second.floatValue();
		} else if (second instanceof Long || first instanceof Long) {
			return first.longValue() / second.longValue();
		} else {
			return first.intValue() / second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Remainder value.
	 */
	@Nonnull
	public static Number rem(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Double || first instanceof Double) {
			return first.doubleValue() % second.doubleValue();
		} else if (second instanceof Float || first instanceof Float) {
			return first.floatValue() % second.floatValue();
		} else if (second instanceof Long || first instanceof Long) {
			return first.longValue() % second.longValue();
		} else {
			return first.intValue() % second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Value where matching bits remain.
	 */
	@Nonnull
	public static Number and(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Long || first instanceof Long) {
			return first.longValue() & second.longValue();
		} else {
			return first.intValue() & second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Value where all active bits remain.
	 */
	@Nonnull
	public static Number or(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Long || first instanceof Long) {
			return first.longValue() | second.longValue();
		} else {
			return first.intValue() | second.intValue();
		}
	}

	/**
	 * @param first
	 * 		First value.
	 * @param second
	 * 		Second value.
	 *
	 * @return Value where non-matching bits remain.
	 */
	@Nonnull
	public static Number xor(@Nonnull Number first, @Nonnull Number second) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (second instanceof Long || first instanceof Long) {
			return first.longValue() ^ second.longValue();
		} else {
			return first.intValue() ^ second.intValue();
		}
	}

	/**
	 * @param value
	 * 		Numeric value.
	 *
	 * @return Negated value.
	 */
	@Nonnull
	public static Number neg(@Nonnull Number value) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (value instanceof Double) {
			return -value.doubleValue();
		} else if (value instanceof Float) {
			return -value.floatValue();
		} else if (value instanceof Long) {
			return -value.longValue();
		} else {
			return -value.intValue();
		}
	}

	/**
	 * @param value
	 * 		Numeric value.
	 * @param shift
	 * 		Value to shift by.
	 *
	 * @return Shifted value.
	 */
	@Nonnull
	public static Number shiftLeft(@Nonnull Number value, @Nonnull Number shift) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (value instanceof Long) {
			return value.longValue() << shift.longValue();
		} else {
			return value.intValue() << shift.intValue();
		}
	}

	/**
	 * @param value
	 * 		Numeric value.
	 * @param shift
	 * 		Value to shift by.
	 *
	 * @return Shifted value.
	 */
	@Nonnull
	public static Number shiftRight(@Nonnull Number value, @Nonnull Number shift) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (value instanceof Long) {
			return value.longValue() >> shift.longValue();
		} else {
			return value.intValue() >> shift.intValue();
		}
	}

	/**
	 * @param value
	 * 		Numeric value.
	 * @param shift
	 * 		Value to shift by.
	 *
	 * @return Shifted value.
	 */
	@Nonnull
	public static Number shiftRightU(@Nonnull Number value, @Nonnull Number shift) {
		// Check for widest types first, go down the type list to narrower types until reaching int.
		if (value instanceof Long) {
			return value.longValue() >>> shift.longValue();
		} else {
			return value.intValue() >>> shift.intValue();
		}
	}

	/**
	 * Fast {@link Math#pow(double, double)} for {@code int}.
	 *
	 * @param base
	 * 		Base value.
	 * @param exp
	 * 		Exponent power.
	 *
	 * @return {@code base^exp}.
	 */
	public static int intPow(int base, int exp) {
		if (exp < 0) throw new IllegalArgumentException("Exponent must be positive");
		int result = 1;
		while (true) {
			if ((exp & 1) != 0) result *= base;
			if ((exp >>= 1) == 0) break;
			base *= base;
		}
		return result;
	}

	/**
	 * @param value
	 * 		Base value.
	 * @param min
	 * 		Clamp min.
	 * @param max
	 * 		Clamp max.
	 *
	 * @return Value, or min if value below min, or max if value above max.
	 */
	public static int intClamp(int value, int min, int max) {
		if (value > max)
			return max;
		if (value < min)
			return min;
		return value;
	}

	/**
	 * @param value
	 * 		Base value.
	 * @param min
	 * 		Clamp min.
	 * @param max
	 * 		Clamp max.
	 *
	 * @return Value, or min if value below min, or max if value above max.
	 */
	public static double doubleClamp(double value, double min, double max) {
		if (value > max)
			return max;
		if (value < min)
			return min;
		return value;
	}

	/**
	 * @param i
	 * 		Some value.
	 *
	 * @return {@code i != 0}
	 */
	public static boolean isNonZero(int i) {
		return i != 0;
	}

	/**
	 * @param i
	 * 		Some value.
	 *
	 * @return {@code i == 0}
	 */
	public static boolean isZero(int i) {
		return i == 0;
	}

	/**
	 * @param a
	 * 		Some value.
	 * @param b
	 * 		Another value.
	 *
	 * @return {@code true} when they have the same sign.
	 */
	public static boolean haveSameSign(int a, int b) {
		return a * b > 0;
	}
}
