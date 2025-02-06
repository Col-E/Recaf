package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.Value;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Base type of value capable of recording exact content
 * when all control flow paths converge on a single use case.
 *
 * @author Matt Coley
 */
public sealed interface ReValue extends Value permits IntValue, FloatValue, DoubleValue, LongValue, ObjectValue, UninitializedValue {
	/**
	 * @param value
	 * 		ASM constant value in a LDC instruction.
	 *
	 * @return A {@link ReValue} wrapper of the given input.
	 *
	 * @throws IllegalValueException
	 * 		When the value could not be mapped to a {@link ReValue}.
	 * @see LdcInsnNode#cst Possible values
	 */
	@Nonnull
	static ReValue ofConstant(@Nullable Object value) throws IllegalValueException {
		return switch (value) {
			case Integer i -> IntValue.of(i);
			case Float f -> FloatValue.of(f);
			case Long l -> LongValue.of(l);
			case Double d -> DoubleValue.of(d);
			case String s -> ObjectValue.string(s);
			case Handle handle -> ObjectValue.VAL_METHOD_HANDLE;
			case Type type -> {
				int sort = type.getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY)
					yield ObjectValue.VAL_CLASS;
				if (sort == Type.METHOD)
					yield ObjectValue.VAL_METHOD_TYPE;
				throw new IllegalValueException("Illegal LDC value " + value);
			}
			case ConstantDynamic constantDynamic -> {
				Type dynamicType = Type.getType(constantDynamic.getDescriptor());
				ReValue dynamicValue = ofType(dynamicType, Nullness.NOT_NULL);
				if (dynamicValue == null)
					throw new IllegalValueException("Illegal LDC dynamic value descriptor " + dynamicType);
				yield dynamicValue;
			}
			case null, default -> throw new IllegalValueException("Illegal LDC value " + value);
		};
	}

	/**
	 * @param type
	 * 		Type of value to create a new generic value for.
	 * @param nullness
	 * 		Nullability state of the value.
	 *
	 * @return Value of the given type.
	 *
	 * @throws IllegalValueException
	 * 		When the type could not be mapped to a {@link ReValue}.
	 */
	@Nullable
	static ReValue ofType(@Nullable Type type, @Nonnull Nullness nullness) throws IllegalValueException {
		if (type == null)
			return UninitializedValue.UNINITIALIZED_VALUE;
		return switch (type.getSort()) {
			case Type.VOID -> null;
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> IntValue.UNKNOWN;
			case Type.FLOAT -> FloatValue.UNKNOWN;
			case Type.LONG -> LongValue.UNKNOWN;
			case Type.DOUBLE -> DoubleValue.UNKNOWN;
			case Type.ARRAY -> ArrayValue.of(type, nullness);
			case Type.OBJECT -> ObjectValue.object(type, nullness);
			default -> throw new IllegalValueException("Invalid type for new value: " + type);
		};
	}

	/**
	 * @param type
	 * 		Type of value to create a new generic value for.
	 *
	 * @return Value of the given type with the default value <i>({@code 0} for primitives, {@code null} for objects/arrays)</i>.
	 *
	 * @throws IllegalValueException
	 * 		When the type could not be mapped to a {@link ReValue}.
	 */
	@Nonnull
	static ReValue ofTypeDefaultValue(@Nonnull Type type) throws IllegalValueException {
		return switch (type.getSort()) {
			case Type.VOID -> throw new IllegalValueException("Cannot create default value for 'void' type");
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> IntValue.VAL_0;
			case Type.FLOAT -> FloatValue.VAL_0;
			case Type.LONG -> LongValue.VAL_0;
			case Type.DOUBLE -> DoubleValue.VAL_0;
			case Type.ARRAY -> ArrayValue.of(type, Nullness.NULL);
			case Type.OBJECT -> ObjectValue.object(type, Nullness.NULL);
			default -> throw new IllegalValueException("Invalid type for new default value: " + type);
		};
	}

	/**
	 * @return {@code true} when the exact content is known.
	 */
	boolean hasKnownValue();

	/**
	 * @return Type of value content.
	 */
	@Nullable
	Type type();

	/**
	 * @param other
	 * 		Other value to merge with.
	 *
	 * @return Merged value.
	 */
	@Nonnull
	ReValue mergeWith(@Nonnull ReValue other);
}
