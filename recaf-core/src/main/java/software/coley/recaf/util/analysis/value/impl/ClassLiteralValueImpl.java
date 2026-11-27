package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

/**
 * Class literal value holder implementation.
 *
 * @author Matt Coley
 */
public class ClassLiteralValueImpl implements ObjectValue {
	private final Type literal;

	/**
	 * @param literal
	 * 		Type of the class literal.
	 * 		Must be an object or array type.
	 */
	public ClassLiteralValueImpl(@Nonnull Type literal) {
		int sort = literal.getSort();
		if (sort != Type.OBJECT && sort != Type.ARRAY)
			throw new IllegalArgumentException("Class literal must be an object or array type: " + literal);
		this.literal = literal;
	}

	/**
	 * @return Type of the class literal this value was loaded from.
	 */
	@Nonnull
	public Type literal() {
		return literal;
	}

	@Override
	public boolean hasKnownValue() {
		return false;
	}

	@Nonnull
	@Override
	public Type type() {
		return Types.CLASS_TYPE;
	}

	@Nonnull
	@Override
	public Nullness nullness() {
		return Nullness.NOT_NULL;
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) throws IllegalValueException {
		if (other == UninitializedValue.UNINITIALIZED_VALUE)
			return other;
		if (other instanceof ObjectValue otherObject) {
			if (equals(otherObject))
				return this;
			return ObjectValue.object(Types.CLASS_TYPE, nullness().mergeWith(otherObject.nullness()));
		}
		throw new IllegalValueException("Cannot merge with: " + other);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ClassLiteralValueImpl other = (ClassLiteralValueImpl) o;

		return literal.equals(other.literal);
	}

	@Override
	public int hashCode() {
		return literal.hashCode();
	}

	@Override
	public String toString() {
		return literal.getClassName() + ".class";
	}
}
