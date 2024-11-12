package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.LongValue;

import java.util.OptionalLong;

/**
 * Long value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LongValueImpl implements LongValue {
	private final OptionalLong value;

	public LongValueImpl(long value) {
		this.value = OptionalLong.of(value);
	}

	public LongValueImpl() {
		this.value = OptionalLong.empty();
	}

	@Nonnull
	@Override
	public OptionalLong value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LongValueImpl other = (LongValueImpl) o;

		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return type().getInternalName() + ":" + (value.isPresent() ? value.getAsLong() : "?");
	}
}
