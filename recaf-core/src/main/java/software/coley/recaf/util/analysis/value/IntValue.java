package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.value.impl.IntValueImpl;

import java.util.OptionalInt;

/**
 * Value capable of recording exact integer content.
 *
 * @author Matt Coley
 */
public non-sealed interface IntValue extends ReValue {
	IntValue UNKNOWN = new IntValueImpl();
	IntValue VAL_MAX = new IntValueImpl(Integer.MAX_VALUE);
	IntValue VAL_MIN = new IntValueImpl(Integer.MIN_VALUE);
	IntValue VAL_M1 = new IntValueImpl(-1);
	IntValue VAL_0 = new IntValueImpl(0);
	IntValue VAL_1 = new IntValueImpl(1);
	IntValue VAL_2 = new IntValueImpl(2);
	IntValue VAL_3 = new IntValueImpl(3);
	IntValue VAL_4 = new IntValueImpl(4);
	IntValue VAL_5 = new IntValueImpl(5);

	/**
	 * @param value
	 * 		Integer value to hold.
	 *
	 * @return Integer value holding the exact content.
	 */
	@Nonnull
	static IntValue of(int value) {
		return switch (value) {
			case Integer.MAX_VALUE -> VAL_MAX;
			case Integer.MIN_VALUE -> VAL_MIN;
			case -1 -> VAL_M1;
			case 0 -> VAL_0;
			case 1 -> VAL_1;
			case 2 -> VAL_2;
			case 3 -> VAL_3;
			case 4 -> VAL_4;
			case 5 -> VAL_5;
			default -> new IntValueImpl(value);
		};
	}

	/**
	 * @return Integer content of value. Empty if {@link #hasKnownValue() not known}.
	 */
	@Nonnull
	OptionalInt value();

	@Override
	default boolean hasKnownValue() {
		return value().isPresent();
	}

	@Nonnull
	@Override
	default Type type() {
		return Type.INT_TYPE;
	}

	@Override
	default int getSize() {
		return 1;
	}

	@Nonnull
	default IntValue add(int incr) {
		OptionalInt value = value();
		if (value.isPresent()) return of(value.getAsInt() + incr);
		return UNKNOWN;
	}

	@Nonnull
	default IntValue add(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() + otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue sub(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() - otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue mul(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() * otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue div(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) {
			int otherLiteral = otherValue.getAsInt();
			if (otherLiteral == 0) return UNKNOWN; // We'll just pretend this works
			return of(value.getAsInt() / otherLiteral);
		}
		return UNKNOWN;
	}

	@Nonnull
	default IntValue and(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() & otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue or(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() | otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue xor(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() ^ otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue rem(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() % otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue shl(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() << otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue shr(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() >> otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue ushr(@Nonnull IntValue other) {
		OptionalInt value = value();
		OptionalInt otherValue = other.value();
		if (value.isPresent() && otherValue.isPresent()) return of(value.getAsInt() >>> otherValue.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue negate() {
		OptionalInt value = value();
		if (value.isPresent()) return of(value.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castByte() {
		OptionalInt value = value();
		if (value.isPresent()) return of((byte) value.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castChar() {
		OptionalInt value = value();
		if (value.isPresent()) return of((char) value.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default IntValue castShort() {
		OptionalInt value = value();
		if (value.isPresent()) return of((short) value.getAsInt());
		return UNKNOWN;
	}

	@Nonnull
	default FloatValue castFloat() {
		OptionalInt value = value();
		if (value.isPresent()) return FloatValue.of(value.getAsInt());
		return FloatValue.UNKNOWN;
	}

	@Nonnull
	default DoubleValue castDouble() {
		OptionalInt value = value();
		if (value.isPresent()) return DoubleValue.of(value.getAsInt());
		return DoubleValue.UNKNOWN;
	}

	@Nonnull
	default LongValue castLong() {
		OptionalInt value = value();
		if (value.isPresent()) return LongValue.of(value.getAsInt());
		return LongValue.UNKNOWN;
	}
}
