package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.value.impl.FloatValueImpl;

import java.util.OptionalDouble;

/**
 * Value capable of recording exact floating point content.
 *
 * @author Matt Coley
 */
public non-sealed interface FloatValue extends ReValue {
	FloatValue UNKNOWN = new FloatValueImpl();
	FloatValue VAL_MAX = new FloatValueImpl(Float.MAX_VALUE);
	FloatValue VAL_MIN = new FloatValueImpl(Float.MIN_VALUE);
	FloatValue VAL_M1 = new FloatValueImpl(-1);
	FloatValue VAL_0 = new FloatValueImpl(0);
	FloatValue VAL_1 = new FloatValueImpl(1);
	FloatValue VAL_2 = new FloatValueImpl(2);

	/**
	 * @param value
	 * 		Float value to hold.
	 *
	 * @return Float value holding the exact content.
	 */
	@Nonnull
	static FloatValue of(float value) {
		if (value == 0) return VAL_0;
		else if (value == 1) return VAL_1;
		else if (value == -1) return VAL_M1;
		else if (value == 2) return VAL_2;
		else if (value == Float.MAX_VALUE) return VAL_MAX;
		else if (value == Float.MIN_VALUE) return VAL_MIN;
		return new FloatValueImpl(value);
	}

	/**
	 * @return Float content of value. Empty if {@link #hasKnownValue() not known}.
	 *
	 * @implNote Java does not have an {@code OptionalFloat}.
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
		return Type.FLOAT_TYPE;
	}

	@Override
	default int getSize() {
		return 1;
	}

	@Nonnull
	default FloatValue add(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return of((float) (value.getAsDouble() + otherValue.getAsDouble()));
		return UNKNOWN;
	}

	@Nonnull
	default FloatValue sub(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return of((float) (value.getAsDouble() - otherValue.getAsDouble()));
		return UNKNOWN;
	}

	@Nonnull
	default FloatValue mul(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return of((float) (value.getAsDouble() * otherValue.getAsDouble()));
		return UNKNOWN;
	}

	@Nonnull
	default FloatValue div(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			double otherLiteral = otherValue.getAsDouble();
			if (otherLiteral == 0) return UNKNOWN; // We'll just pretend this works
			return of((float) (value.getAsDouble() / otherLiteral));
		}
		return UNKNOWN;
	}

	@Nonnull
	default IntValue cmpg(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			float f1 = (float) value.getAsDouble();
			float f2 = (float) otherValue.getAsDouble();
			if (Float.isNaN(f1) || Float.isNaN(f2)) return IntValue.VAL_1;
			return IntValue.of(Float.compare(f1, f2));
		}
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default IntValue cmpl(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			float f1 = (float) value.getAsDouble();
			float f2 = (float) otherValue.getAsDouble();
			if (Float.isNaN(f1) || Float.isNaN(f2)) return IntValue.VAL_M1;
			return IntValue.of(Float.compare(f1, f2));
		}
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default FloatValue rem(@Nonnull FloatValue other) {
		OptionalDouble value = value();
		OptionalDouble otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return of((float) (value.getAsDouble() % otherValue.getAsDouble()));
		return UNKNOWN;
	}

	@Nonnull
	default FloatValue negate() {
		OptionalDouble value = value();
		if (value.isPresent()) return of((float) -value.getAsDouble());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castInt() {
		OptionalDouble value = value();
		if (value.isPresent()) return IntValue.of((int) value.getAsDouble());
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default DoubleValue castDouble() {
		OptionalDouble value = value();
		if (value.isPresent()) return DoubleValue.of(value.getAsDouble());
		return DoubleValue.UNKNOWN;
	}

	@Nonnull
	default LongValue castLong() {
		OptionalDouble value = value();
		if (value.isPresent()) return LongValue.of((long) value.getAsDouble());
		return LongValue.UNKNOWN;
	}
}
