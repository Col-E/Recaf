package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.Objects;
import java.util.Optional;

/**
 * Boxed object value holder implementation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class ObjectValueBoxImpl<T> extends ObjectValueImpl {
	private final Optional<T> value;

	public ObjectValueBoxImpl(@Nonnull Type type, @Nonnull Nullness nullness) {
		super(type, nullness);
		value = Optional.empty();
	}

	public ObjectValueBoxImpl(@Nonnull Type type, @Nullable T value) {
		super(type, value == null ? Nullness.NULL : Nullness.NOT_NULL);
		this.value = Optional.ofNullable(value);
	}

	@Nonnull
	protected abstract ObjectValueBoxImpl<T> wrap(@Nullable T value);

	@Nonnull
	protected abstract ObjectValueBoxImpl<T> wrapUnknown(@Nonnull Nullness nullness);

	@Override
	public boolean hasKnownValue() {
		return nullness() == Nullness.NOT_NULL && value.isPresent();
	}

	@Nullable
	public T unbox() {
		return value().orElseThrow();
	}

	@Nonnull
	public Optional<T> value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ObjectValueBoxImpl<?> other = (ObjectValueBoxImpl<?>) o;

		return value().equals(other.value());
	}

	@Override
	public int hashCode() {
		int value = type().hashCode();
		value = 31 * value + value().hashCode();
		return value;
	}

	@Override
	public String toString() {
		return type().getInternalName() + ":" + (hasKnownValue() ? unbox() : "<?>");
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) throws IllegalValueException {
		if (other == UninitializedValue.UNINITIALIZED_VALUE)
			return other;
		else if (other instanceof ObjectValueBoxImpl<?> otherBoxed) {
			if (type().equals(otherBoxed.type()) && value().isPresent() && otherBoxed.value().isPresent()) {
				T v = value().get();
				T otherV = (T) otherBoxed.value().get();
				if (Objects.equals(v, otherBoxed))
					return wrap(v);
			}
			return wrapUnknown(nullness().mergeWith(otherBoxed.nullness()));
		}
		throw new IllegalValueException("Cannot merge with: " + other);
	}
}
