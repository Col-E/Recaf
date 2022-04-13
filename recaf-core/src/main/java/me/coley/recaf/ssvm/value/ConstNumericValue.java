package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.*;

/**
 * A constant numeric value.
 *
 * @author Matt Coley
 */
public class ConstNumericValue extends SimpleDelegatingValue<NumericValue> implements ConstValue {
	/**
	 * @param delegate
	 * 		Wrapped numeric value.
	 */
	public ConstNumericValue(NumericValue delegate) {
		super(delegate);
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofInt(int value) {
		return new ConstNumericValue(IntValue.of(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofLong(long value) {
		return new ConstNumericValue(LongValue.of(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofFloat(float value) {
		return new ConstNumericValue(new FloatValue(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofDouble(double value) {
		return new ConstNumericValue(new DoubleValue(value));
	}
}
