package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.IntValue;

import java.util.OptionalInt;

/**
 * Integer value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class IntValueImpl implements IntValue {
	private final OptionalInt value;

	public IntValueImpl() {
		this.value = OptionalInt.empty();
	}

	public IntValueImpl(int value) {
		this.value = OptionalInt.of(value);
	}

	@Nonnull
	@Override
	public OptionalInt value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IntValueImpl other = (IntValueImpl) o;

		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return type().getInternalName() + ":" + (value.isPresent() ? value.getAsInt() : "?");
	}
}
