package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedBooleanValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedByteValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedCharacterValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedDoubleValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedFloatValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedIntegerValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedLongValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedShortValueImpl;
import software.coley.recaf.util.analysis.value.impl.ObjectValueImpl;
import software.coley.recaf.util.analysis.value.impl.StringValueImpl;

import java.lang.reflect.Array;

/**
 * Object value that has a real instance backing it.
 * This is used to call real methods on the instance for specific supported APIs.
 *
 * @param <T>
 * 		Type of the real instance.
 *
 * @author Matt Coley
 */
public class InstancedObjectValue<T> extends ObjectValueImpl {
	private T realInstance;

	/**
	 * New instanced value without the real instance specified immediately.
	 *
	 * @param type
	 * 		Object type.
	 */
	public InstancedObjectValue(@Nonnull Type type) {
		super(type, Nullness.NOT_NULL);
	}

	/**
	 * New instanced value with the real instance specified immediately.
	 *
	 * @param value
	 * 		Real object instance.
	 */
	public InstancedObjectValue(@Nonnull T value) {
		super(Type.getType(value.getClass()), Nullness.NOT_NULL);
		setRealInstance(value);
	}

	/**
	 * Assign the real instance backing this value.
	 *
	 * @param realInstance
	 * 		Real object instance.
	 *
	 * @see InstanceMapper
	 * @see InstanceFactory
	 */
	public void setRealInstance(@Nonnull T realInstance) {
		this.realInstance = realInstance;
	}

	/**
	 * @return Real object instance backing this value, or {@code null} if no instance is assigned.
	 */
	@Nullable
	public T getRealInstance() {
		return realInstance;
	}

	/**
	 * Attempt to unmap the backing instance into a mapped value.
	 * If the instance is not supported for mapping, then this will return {@code this}.
	 *
	 * @return Mapped {@link ReValue} representation of the backing {@link #getRealInstance() real instance}.
	 */
	@Nonnull
	public ReValue unmap() {
		ReValue unmapped = unmap(type(), realInstance);
		if (unmapped != null)
			return unmapped;
		return this;
	}

	@Nullable
	private static ReValue unmap(@Nonnull Type type, @Nullable Object instance) {
		try {
			if (instance == null)
				throw new IllegalValueException("No real instance available for unmapping.");
			if (Types.isPrimitive(type))
				return ReValue.ofConstant(instance);
			if (instance instanceof CharSequence cs)
				return new StringValueImpl(cs.toString());
			String descriptor = type.getDescriptor();
			if (Types.isBoxedPrimitive(descriptor)) switch (descriptor) {
				case "Ljava/lang/Boolean;" -> {
					return new BoxedBooleanValueImpl((Boolean) instance);
				}
				case "Ljava/lang/Byte;" -> {
					return new BoxedByteValueImpl((Byte) instance);
				}
				case "Ljava/lang/Short;" -> {
					return new BoxedShortValueImpl((Short) instance);
				}
				case "Ljava/lang/Character;" -> {
					return new BoxedCharacterValueImpl((Character) instance);
				}
				case "Ljava/lang/Integer;" -> {
					return new BoxedIntegerValueImpl((Integer) instance);
				}
				case "Ljava/lang/Long;" -> {
					return new BoxedLongValueImpl((Long) instance);
				}
				case "Ljava/lang/Float;" -> {
					return new BoxedFloatValueImpl((Float) instance);
				}
				case "Ljava/lang/Double;" -> {
					return new BoxedDoubleValueImpl((Double) instance);
				}
			}
			if (type.getSort() == Type.ARRAY) {
				Type componentType = Types.undimension(type);
				return new ArrayValueImpl(type, Nullness.NOT_NULL, Array.getLength(instance), i -> unmap(componentType, Array.get(instance, i)));
			}
		} catch (IllegalValueException _) {}
		return null;
	}

	@Override
	public boolean hasKnownValue() {
		return realInstance != null;
	}

	@Override
	public String toString() {
		return super.toString() + " : " + realInstance;
	}
}
