package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.Value;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReInterpreter;

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
			case Character c -> IntValue.of(c);
			case Byte b -> IntValue.of(b);
			case Short s -> IntValue.of(s);
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
	 * @param value
	 * 		Some value that may be a primitive.
	 * @param v
	 * 		Primitive value to compare against.
	 *
	 * @return {@code true} when the value is a primitive,
	 * and the primitive value is equal to the given literal value.
	 */
	static boolean isPrimitiveEqualTo(@Nonnull ReValue value, int v) {
		return switch (value) {
			case DoubleValue x -> x.isEqualTo(v);
			case FloatValue x -> x.isEqualTo(v);
			case IntValue x -> x.isEqualTo(v);
			case LongValue x -> x.isEqualTo(v);
			case ObjectValue _, UninitializedValue _ -> false;
		};
	}

	/**
	 * @return {@code true} when the exact content is known.
	 * {@code null} does not count if this is an {@link ObjectValue}
	 * and for that you should use {@link ObjectValue#isNull()}.
	 */
	boolean hasKnownValue();

	/**
	 * @return Type of value content.
	 */
	@Nullable
	Type type();

	/**
	 * Called from {@link ReInterpreter#merge(ReValue, ReValue)} only when our value's type is a looser type than the given other type.
	 * For instance, if we are a {@code ArrayList} we could see {@code List} as the {@code other} value, but never the other way around.
	 *
	 * @param other
	 * 		Other value to merge with.
	 * 		It should be assumed that this other value's type is the same as, or a child type of our {@link #type()}.
	 *
	 * @return Merged value.
	 *
	 * @throws IllegalValueException
	 * 		When the given value cannot be merged with this one.
	 */
	@Nonnull
	ReValue mergeWith(@Nonnull ReValue other) throws IllegalValueException;
}
