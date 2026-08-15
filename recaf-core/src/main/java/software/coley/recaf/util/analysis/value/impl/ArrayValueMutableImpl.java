package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Mutable array value holder implementation.
 * <p>
 * Generally only used within {@link Evaluator} and not intended for frame-generation usage.
 * Frame generation uses the immutable {@link ArrayValueImpl} variant.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ArrayValueMutableImpl implements ArrayValue {
	private final Type type;
	private final Nullness nullness;
	private final OptionalInt length;
	private final List<ReValue> contents;

	private ArrayValueMutableImpl(@Nonnull ArrayValue source) {
		type = source.type();
		nullness = source.nullness();
		length = source.getFirstDimensionLength();

		// Copy contents if known, otherwise leave null to indicate unknown contents.
		if (length.isPresent()) {
			contents = new ArrayList<>(length.getAsInt());
			for (int i = 0; i < length.getAsInt(); i++) {
				ReValue value = Objects.requireNonNull(source.getValue(i));
				contents.add(value instanceof ArrayValue array ? wrap(array) : value);
			}
		} else {
			contents = null;
		}
	}

	/**
	 * @param value
	 * 		Array value to wrap in a mutable implementation.
	 *
	 * @return Mutable array value implementation.
	 */
	@Nonnull
	public static ArrayValue wrap(@Nonnull ArrayValue value) {
		return value instanceof ArrayValueMutableImpl ? value : new ArrayValueMutableImpl(value);
	}

	@Override
	public ArrayValue setValue(int index, @Nonnull ReValue value) {
		if (hasKnownValue())
			contents.set(index, value instanceof ArrayValue array ? wrap(array) : value);
		return this;
	}

	@Override
	public ArrayValue updatedCopyIfContained(@Nonnull ReValue originalValue, @Nonnull ReValue updatedValue) {
		if (hasKnownValue()) {
			for (int i = 0; i < contents.size(); i++) {
				ReValue content = contents.get(i);

				// Case 1: The value is a direct entry in this array.
				if (content == originalValue)
					return setValue(i, updatedValue);

					// Case 2: This array is multidimensional and the value is in a nested sub array.
				else if (content instanceof ArrayValue subArray) {
					ArrayValue updatedSubArray = subArray.updatedCopyIfContained(originalValue, updatedValue);
					if (subArray != updatedSubArray)
						return setValue(i, updatedSubArray);
				}
			}
		}

		// Not contained, no changes needed.
		return this;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public boolean hasKnownValue() {
		return nullness == Nullness.NOT_NULL
				&& length.isPresent()
				&& contents != null
				&& contents.stream().allMatch(value -> value.hasKnownValue()
				|| value instanceof ObjectValue object && object.isNull());
	}

	@Override
	public ReValue mergeWith(@Nonnull ReValue other) throws IllegalValueException {
		if (other == UninitializedValue.UNINITIALIZED_VALUE)
			return other;
		ArrayValueImpl snapshot = length.isPresent()
				? new ArrayValueImpl(type, nullness, length.getAsInt(), contents::get)
				: new ArrayValueImpl(type, nullness);
		return snapshot.mergeWith(other);
	}

	@Override
	public Nullness nullness() {
		return nullness;
	}

	@Override
	public OptionalInt getFirstDimensionLength() {
		return length;
	}

	@Override
	public ReValue getValue(int index) {
		if (index < 0 || index >= length.orElse(0)) {
			try {
				return Objects.requireNonNull(ReValue.ofType(type.getElementType(), Nullness.UNKNOWN));
			} catch (IllegalValueException ex) {
				throw new IllegalStateException("Failed creating unknown array element value", ex);
			}
		}
		return contents == null ? null : contents.get(index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ArrayValueMutableImpl that)) return false;

		if (!type.equals(that.type)) return false;
		if (nullness != that.nullness) return false;
		if (!length.equals(that.length)) return false;
		return Objects.equals(contents, that.contents);
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + nullness.hashCode();
		result = 31 * result + length.hashCode();
		result = 31 * result + (contents != null ? contents.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return type().getInternalName();
	}
}
