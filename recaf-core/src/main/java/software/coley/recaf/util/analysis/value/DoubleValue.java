package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.value.impl.DoubleValueImpl;

import java.util.OptionalDouble;

/**
 * Value capable of recording exact floating double precision content.
 *
 * @author Matt Coley
 */
public non-sealed interface DoubleValue extends ReValue {
	DoubleValue UNKNOWN = new DoubleValueImpl();
	DoubleValue VAL_MAX = new DoubleValueImpl(Double.MAX_VALUE);
	DoubleValue VAL_MIN = new DoubleValueImpl(Double.MIN_VALUE);
	DoubleValue VAL_M1 = new DoubleValueImpl(-1);
	DoubleValue VAL_0 = new DoubleValueImpl(0);
	DoubleValue VAL_1 = new DoubleValueImpl(1);

	/**
	 * @param value
	 * 		Double value to hold.
	 *
	 * @return Double value holding the exact content.
	 */
	@Nonnull
	static DoubleValue of(double value) {
		if (value == 0) return VAL_0;
		else if (value == 1) return VAL_1;
		else if (value == -1) return VAL_M1;
		else if (value == Double.MAX_VALUE) return VAL_MAX;
		else if (value == Double.MIN_VALUE) return VAL_MIN;
		return new DoubleValueImpl(value);
	}

	/**
	 * @return Double content of value. Empty if {@link #hasKnownValue() not known}.
	 */
	@Nonnull
	OptionalDouble value();

	@Override
	default boolean hasKnownValue() {
		return value().isPresent();
	}

	@Nonnull
	@Override
	default Type type() {
		return Type.DOUBLE_TYPE;
	}

	@Override
	default int getSize() {
		return 2;
	}

	@Nonnull
	default DoubleValue add(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsDouble() + otherValue.getAsDouble());
		return UNKNOWN;
	}

	@Nonnull
	default DoubleValue sub(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsDouble() - otherValue.getAsDouble());
		return UNKNOWN;
	}

	@Nonnull
	default DoubleValue mul(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsDouble() * otherValue.getAsDouble());
		return UNKNOWN;
	}

	@Nonnull
	default DoubleValue div(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			double otherLiteral = otherValue.getAsDouble();
			if (otherLiteral == 0) return UNKNOWN; // We'll just pretend this works
			return of((value.getAsDouble() / otherLiteral));
		}
		return UNKNOWN;
	}

	@Nonnull
	default IntValue cmpg(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			double f1 = value.getAsDouble();
			double f2 = otherValue.getAsDouble();
			if (Double.isNaN(f1) || Double.isNaN(f2)) return IntValue.VAL_1;
			return IntValue.of(Double.compare(f1, f2));
		}
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default IntValue cmpl(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			double f1 = value.getAsDouble();
			double f2 = otherValue.getAsDouble();
			if (Double.isNaN(f1) || Double.isNaN(f2)) return IntValue.VAL_M1;
			return IntValue.of(Double.compare(f1, f2));
		}
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default DoubleValue rem(@Nonnull DoubleValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return of((float) (value.getAsDouble() % otherValue.getAsDouble()));
		return UNKNOWN;
	}

	@Nonnull
	default DoubleValue negate() {
		OptionalDouble value = value();
		if (value.isPresent()) return of(-value.getAsDouble());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castInt() {
		OptionalDouble value = value();
		if (value.isPresent()) return IntValue.of((int) value.getAsDouble());
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default FloatValue castFloat() {
		OptionalDouble value = value();
		if (value.isPresent()) return FloatValue.of((float) value.getAsDouble());
		return FloatValue.UNKNOWN;
	}

	@Nonnull
	default LongValue castLong() {
		OptionalDouble value = value();
		if (value.isPresent()) return LongValue.of((long) value.getAsDouble());
		return LongValue.UNKNOWN;
	}
}
