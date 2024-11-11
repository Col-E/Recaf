package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.OptionalDouble;

/**
 * Double value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DoubleValueImpl implements DoubleValue {
	private final OptionalDouble value;

	public DoubleValueImpl(double value) {
		this.value = OptionalDouble.of(value);
	}

	public DoubleValueImpl() {
		this.value = OptionalDouble.empty();
	}

	@Nonnull
	@Override
	public OptionalDouble value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DoubleValueImpl other = (DoubleValueImpl) o;

		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return type().getInternalName() + ":" + (value.isPresent() ? value.getAsDouble() : "?");
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) {
		if (other instanceof DoubleValue otherDouble) {
			if (value().isPresent() && otherDouble.value().isPresent()) {
				double d = value().getAsDouble();
				double otherD = otherDouble.value().getAsDouble();
				if (d == otherD)
					return DoubleValue.of(d);
			}
			return DoubleValue.UNKNOWN;
		}
		throw new IllegalStateException("Cannot merge with: " + other);
	}
}
