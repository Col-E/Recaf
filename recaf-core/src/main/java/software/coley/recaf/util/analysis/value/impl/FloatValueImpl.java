package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.FloatValue;

import java.util.OptionalDouble;

/**
 * Float value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FloatValueImpl implements FloatValue {
	private final OptionalDouble value;

	public FloatValueImpl(float value) {
		this.value = OptionalDouble.of(value);
	}

	public FloatValueImpl(double value) {
		this.value = OptionalDouble.of(value);
	}

	public FloatValueImpl() {
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

		FloatValueImpl other = (FloatValueImpl) o;

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
}
