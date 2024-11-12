package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.value.impl.LongValueImpl;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Value capable of recording exact long content.
 *
 * @author Matt Coley
 */
public non-sealed interface LongValue extends ReValue {
	LongValue UNKNOWN = new LongValueImpl();
	LongValue VAL_MAX = new LongValueImpl(Long.MAX_VALUE);
	LongValue VAL_MIN = new LongValueImpl(Long.MIN_VALUE);
	LongValue VAL_M1 = new LongValueImpl(-1);
	LongValue VAL_0 = new LongValueImpl(0);
	LongValue VAL_1 = new LongValueImpl(1);

	/**
	 * @param value
	 * 		Long value to hold.
	 *
	 * @return Long value holding the exact content.
	 */
	@Nonnull
	static LongValue of(long value) {
		if (value == 0) return VAL_0;
		else if (value == 1) return VAL_1;
		else if (value == -1) return VAL_M1;
		else if (value == Long.MAX_VALUE) return VAL_MAX;
		else if (value == Long.MIN_VALUE) return VAL_MIN;
		return new LongValueImpl(value);
	}

	/**
	 * @return Long content of value. Empty if {@link #hasKnownValue() not known}.
	 */
	@Nonnull
	OptionalLong value();

	@Override
	default boolean hasKnownValue() {
		return value().isPresent();
	}

	@Nonnull
	@Override
	default Type type() {
		return Type.LONG_TYPE;
	}

	@Override
	default int getSize() {
		return 2;
	}

	@Nonnull
	default LongValue add(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() + otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue sub(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() - otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue mul(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() * otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue div(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			long otherLiteral = otherValue.getAsLong();
			if (otherLiteral == 0) return UNKNOWN; // We'll just pretend this works
			return of(value.getAsLong() / otherLiteral);
		}
		return UNKNOWN;
	}

	@Nonnull
	default LongValue and(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() & otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue or(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() | otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue xor(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() ^ otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue cmp(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent())
			return IntValue.of(Long.compare(value.getAsLong(), otherValue.getAsLong()));
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default LongValue rem(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() % otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue shl(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() << otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue shl(@Nonnull IntValue other) {
		OptionalLong value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() << otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue shr(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() >> otherValue.getAsLong());
		return UNKNOWN;
	}


	@Nonnull
	default LongValue shr(@Nonnull IntValue other) {
		OptionalLong value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() >> otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue ushr(@Nonnull LongValue other) {
		OptionalLong value = value();
		OptionalLong otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() >>> otherValue.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue ushr(@Nonnull IntValue other) {
		OptionalLong value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsLong() >>> otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue negate() {
		OptionalLong value = value();
		if (value.isPresent()) return of(-value.getAsLong());
		return UNKNOWN;
	}

	@Nonnull
	default LongValue add(long incr) {
		OptionalLong value = value();
		if (value.isPresent()) return of(value.getAsLong() + incr);
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castInt() {
		OptionalLong value = value();
		if (value.isPresent()) return IntValue.of((int) value.getAsLong());
		return IntValue.UNKNOWN;
	}

	@Nonnull
	default FloatValue castFloat() {
		OptionalLong value = value();
		if (value.isPresent()) return FloatValue.of(value.getAsLong());
		return FloatValue.UNKNOWN;
	}

	@Nonnull
	default DoubleValue castDouble() {
		OptionalLong value = value();
		if (value.isPresent()) return DoubleValue.of(value.getAsLong());
		return DoubleValue.UNKNOWN;
	}
}
