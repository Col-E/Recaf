package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Object value holder implementation.
 *
 * @author Matt Coley
 */
public class ObjectValueImpl implements ObjectValue {
	private final Type type;
	private final Nullness nullness;

	public ObjectValueImpl(@Nonnull Type type, @Nonnull Nullness nullness) {
		this.type = type;
		this.nullness = nullness;
	}

	@Override
	public boolean hasKnownValue() {
		return false;
	}

	@Nonnull
	@Override
	public Type type() {
		return type;
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) {
		if (other instanceof ObjectValue otherObject)
			return ObjectValue.object(type, nullness().mergeWith(otherObject.nullness()));
		throw new IllegalStateException("Cannot merge with: " + other);
	}

	@Nonnull
	@Override
	public Nullness nullness() {
		return nullness;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ObjectValueImpl other = (ObjectValueImpl) o;

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
		return type.getInternalName();
	}
}
