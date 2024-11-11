package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.sql.Types;
import java.util.OptionalInt;

/**
 * Array value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ArrayValueImpl implements ArrayValue {
	private final Type type;
	private final Nullness nullness;
	private final OptionalInt length;

	public ArrayValueImpl(@Nonnull Type type, @Nonnull Nullness nullness) {
		if (type.getSort() != Types.ARRAY) throw new IllegalStateException("Non-array type passed to array-value");
		this.type = type;
		this.nullness = nullness;
		this.length = OptionalInt.empty();
	}

	public ArrayValueImpl(@Nonnull Type type, @Nonnull Nullness nullness, int length) {
		if (type.getSort() != Types.ARRAY) throw new IllegalStateException("Non-array type passed to array-value");
		this.type = type;
		this.nullness = nullness;
		this.length = OptionalInt.of(length);
	}

	@Nonnull
	@Override
	public Type type() {
		return type;
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) {
		if (other instanceof ArrayValue otherArray) {
			if (getFirstDimensionLength().isPresent() && otherArray.getFirstDimensionLength().isPresent()) {
				int dim = getFirstDimensionLength().getAsInt();
				int otherDim = otherArray.getFirstDimensionLength().getAsInt();
				if (dim == otherDim)
					return ArrayValue.of(type, nullness.mergeWith(otherArray.nullness()), dim);
			}
			return ArrayValue.of(type, nullness.mergeWith(otherArray.nullness()));
		}
		throw new IllegalStateException("Cannot merge with: " + other);
	}

	@Nonnull
	@Override
	public Nullness nullness() {
		return nullness;
	}

	@Nonnull
	@Override
	public OptionalInt getFirstDimensionLength() {
		return length;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ArrayValueImpl other = (ArrayValueImpl) o;

		return type.equals(other.type) && nullness.equals(other.nullness);
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + nullness.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return type().getInternalName();
	}
}
