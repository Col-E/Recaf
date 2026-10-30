package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedByteValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedCharacterValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedDoubleValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedFloatValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedIntegerValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedLongValueImpl;
import software.coley.recaf.util.analysis.value.impl.BoxedShortValueImpl;
import software.coley.recaf.util.analysis.value.impl.ObjectValueBoxImpl;

import java.util.OptionalInt;

import static software.coley.recaf.util.Types.*;

/**
 * Utilities for working with {@link ReValue} instances.
 *
 * @author Matt Coley
 */
public final class Values {
	private Values() {}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Byte representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static byte asByte(@Nonnull IntValue value) {
		return (byte) value.value().getAsInt();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Boolean representation of the value.
	 */
	public static boolean asBoolean(@Nonnull IntValue value) {
		return value.isNotEqualTo(0);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Short representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static short asShort(@Nonnull IntValue value) {
		return (short) value.value().getAsInt();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Char representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static char asChar(@Nonnull IntValue value) {
		return (char) value.value().getAsInt();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Int representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static int asInt(@Nonnull IntValue value) {
		return value.value().getAsInt();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Long representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static long asLong(@Nonnull LongValue value) {
		return value.value().getAsLong();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Float representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static float asFloat(@Nonnull FloatValue value) {
		return (float) value.value().getAsDouble();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Double representation of the value.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static double asDouble(@Nonnull DoubleValue value) {
		return value.value().getAsDouble();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return String representation of the value.
	 */
	@Nullable
	public static String asString(@Nonnull ObjectValue value) {
		if (value.isNull())
			return null;
		if (value instanceof StringValue string)
			return string.getText().orElseThrow(() -> new IllegalArgumentException("Unknown string value"));
		if (value instanceof InstancedObjectValue<?> instance && instance.getRealInstance() instanceof String string)
			return string;
		throw new IllegalArgumentException("Unsupported string unwrap: " + value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return String representation of the value, or {@code null} if the value is not a known string.
	 */
	@Nullable
	public static String asStringOrNull(@Nonnull ReValue value) {
		if (!value.hasKnownValue())
			return null;
		try {
			if (value instanceof ObjectValue obj)
				return asString(obj);
		} catch (Exception e) {
			// Ignore, not a known string value.
		}
		return null;
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Raw object representation of the value.
	 */
	@Nullable
	public static Object asObjectLiteral(@Nonnull ObjectValue value) {
		return asObject(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Raw object representation of the value.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T asObject(@Nonnull ObjectValue value) {
		if (value.isNull())
			return null;
		return switch (value) {
			case StringValue string -> (T) asString(string);
			case ObjectValueBoxImpl<?> box -> (T) box.unbox();
			case InstancedObjectValue<?> instanced when instanced.getRealInstance() != null ->
					(T) instanced.getRealInstance();
			default -> throw new IllegalArgumentException("Unsupported object unwrap: " + value);
		};
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped int primitive representation of the value.
	 */
	@Nonnull
	public static IntValue toBooleanValue(boolean value) {
		return IntValue.of(value ? 1 : 0);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped byte primitive representation of the value.
	 */
	@Nonnull
	public static IntValue toByteValue(byte value) {
		return IntValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped char primitive representation of the value.
	 */
	@Nonnull
	public static IntValue toCharValue(char value) {
		return IntValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped short primitive representation of the value.
	 */
	@Nonnull
	public static IntValue toShortValue(short value) {
		return IntValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped int primitive representation of the value.
	 */
	@Nonnull
	public static IntValue toIntValue(int value) {
		return IntValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped long primitive representation of the value.
	 */
	@Nonnull
	public static LongValue toLongValue(long value) {
		return LongValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped float primitive representation of the value.
	 */
	@Nonnull
	public static FloatValue toFloatValue(float value) {
		return FloatValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped double primitive representation of the value.
	 */
	@Nonnull
	public static DoubleValue toDoubleValue(double value) {
		return DoubleValue.of(value);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped String representation of the value.
	 */
	@Nonnull
	public static StringValue toStringValue(@Nullable CharSequence value) {
		return ObjectValue.string(value == null ? null : value.toString());
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Wrapped object representation of the value.
	 */
	@Nonnull
	public static ObjectValue toObjectValue(@Nullable Object value) {
		return switch (value) {
			case null -> ObjectValue.VAL_OBJECT_NULL;
			case CharSequence sequence -> toStringValue(sequence);
			case Character character -> new BoxedCharacterValueImpl(character);
			case Integer number -> new BoxedIntegerValueImpl(number);
			case Long number -> new BoxedLongValueImpl(number);
			case Byte number -> new BoxedByteValueImpl(number);
			case Float number -> new BoxedFloatValueImpl(number);
			case Double number -> new BoxedDoubleValueImpl(number);
			case Short number -> new BoxedShortValueImpl(number);
			default -> throw new IllegalArgumentException("Unsupported Object wrap: " + value);
		};
	}

	/**
	 * @param value
	 * 		Value of an array to consider.
	 * @param type
	 * 		Expected array type of the value.
	 *
	 * @return The {@code value} if it is a known array of the given {@code type}, otherwise {@code null}.
	 */
	@Nullable
	public static ArrayValue asKnownArrayOfType(@Nonnull ReValue value, @Nonnull Type type) {
		if (value instanceof ArrayValue array
				&& type.equals(array.type())
				&& array.isNotNull()
				&& array.getFirstDimensionLength().isPresent())
			return array;
		return null;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of booleans if the value is a known array of type {@code [Z}, otherwise {@code null}.
	 */
	@Nullable
	private static ArrayValue getFullyKnownArray(@Nonnull ReValue value, @Nonnull Type type) {
		ArrayValue array = asKnownArrayOfType(value, type);
		return array != null && array.hasKnownValue() ? array : null;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 * @param type
	 * 		Expected array type of the value.
	 * @param arrayType
	 * 		Expected array class type of the value.
	 * @param <T>
	 * 		Array type.
	 *
	 * @return The {@code value} if it is a known array of the given {@code type}, otherwise {@code null}.
	 */
	@Nullable
	private static <T> T getKnownHostArray(@Nonnull ReValue value, @Nonnull Type type, @Nonnull Class<T> arrayType) {
		if (value instanceof InstancedObjectValue<?> instanced && type.equals(instanced.type())) {
			Object realInstance = instanced.getRealInstance();
			if (arrayType.isInstance(realInstance))
				return arrayType.cast(realInstance);
		}
		return null;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of booleans if the value is a known array of type {@code [Z}, otherwise {@code null}.
	 */
	@Nullable
	public static boolean[] getKnownBooleanArray(@Nonnull ReValue value) {
		boolean[] hostArray = getKnownHostArray(value, ARRAY_1D_BOOLEAN, boolean[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_BOOLEAN);
		return array == null ? null : asBooleanArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of bytes if the value is a known array of type {@code [B}, otherwise {@code null}.
	 */
	@Nullable
	public static byte[] getKnownByteArray(@Nonnull ReValue value) {
		byte[] hostArray = getKnownHostArray(value, ARRAY_1D_BYTE, byte[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_BYTE);
		return array == null ? null : asByteArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of chars if the value is a known array of type {@code [C}, otherwise {@code null}.
	 */
	@Nullable
	public static char[] getKnownCharArray(@Nonnull ReValue value) {
		char[] hostArray = getKnownHostArray(value, ARRAY_1D_CHAR, char[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_CHAR);
		return array == null ? null : asCharArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of ints if the value is a known array of type {@code [I}, otherwise {@code null}.
	 */
	@Nullable
	public static int[] getKnownIntArray(@Nonnull ReValue value) {
		int[] hostArray = getKnownHostArray(value, ARRAY_1D_INT, int[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_INT);
		return array == null ? null : asIntArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of floats if the value is a known array of type {@code [F}, otherwise {@code null}.
	 */
	@Nullable
	public static float[] getKnownFloatArray(@Nonnull ReValue value) {
		float[] hostArray = getKnownHostArray(value, ARRAY_1D_FLOAT, float[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_FLOAT);
		return array == null ? null : asFloatArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of doubles if the value is a known array of type {@code [D}, otherwise {@code null}.
	 */
	@Nullable
	public static double[] getKnownDoubleArray(@Nonnull ReValue value) {
		double[] hostArray = getKnownHostArray(value, ARRAY_1D_DOUBLE, double[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_DOUBLE);
		return array == null ? null : asDoubleArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of longs if the value is a known array of type {@code [J}, otherwise {@code null}.
	 */
	@Nullable
	public static long[] getKnownLongArray(@Nonnull ReValue value) {
		long[] hostArray = getKnownHostArray(value, ARRAY_1D_LONG, long[].class);
		if (hostArray != null)
			return hostArray;
		ArrayValue array = getFullyKnownArray(value, ARRAY_1D_LONG);
		return array == null ? null : asLongArray(array);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of booleans if the value is a known array of {@code [Z}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static boolean[] asBooleanArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asBooleanArray(array);
		return hostArray(value, boolean[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of bytes if the value is a known array of {@code [B}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static byte[] asByteArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asByteArray(array);
		return hostArray(value, byte[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of shorts if the value is a known array of {@code [S}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static short[] asShortArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asShortArray(array);
		return hostArray(value, short[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of chars if the value is a known array of {@code [C}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static char[] asCharArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asCharArray(array);
		return hostArray(value, char[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of ints if the value is a known array of {@code [I}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static int[] asIntArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asIntArray(array);
		return hostArray(value, int[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of floats if the value is a known array of {@code [F}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static float[] asFloatArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asFloatArray(array);
		return hostArray(value, float[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of doubles if the value is a known array of {@code [D}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static double[] asDoubleArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asDoubleArray(array);
		return hostArray(value, double[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of longs if the value is a known array of {@code [J}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static long[] asLongArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asLongArray(array);
		return hostArray(value, long[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of strings if the value is a known array of {@code [Ljava/lang/String;}, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 */
	@Nullable
	public static String[] asStringArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asStringArray(array);

		Object hostValue = hostArray(value, Object[].class);
		if (hostValue instanceof String[] strings)
			return strings;

		Object[] objects = (Object[]) hostValue;
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++)
			strings[i] = objects[i] == null ? null : objects[i].toString();

		return strings;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of objects if the value is a known array of supported object types, otherwise {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length, is not the expected type, or is not a known array.
	 * @see #asObject(ObjectValue)
	 */
	@Nullable
	public static Object[] asObjectArray(@Nonnull ReValue value) {
		if (value instanceof ArrayValue array)
			return asObjectArray(array);
		return hostArray(value, Object[].class);
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of booleans for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static boolean[] asBooleanArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		boolean[] booleans = new boolean[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof IntValue intValue && intValue.hasKnownValue())
				booleans[i] = asBoolean(intValue);
		}
		return booleans;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of bytes for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static byte[] asByteArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof IntValue intValue && intValue.hasKnownValue())
				bytes[i] = asByte(intValue);
		}
		return bytes;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of shorts for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static short[] asShortArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		short[] shorts = new short[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof IntValue intValue && intValue.hasKnownValue())
				shorts[i] = asShort(intValue);
		}
		return shorts;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of chars for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static char[] asCharArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof IntValue intValue && intValue.hasKnownValue())
				chars[i] = asChar(intValue);
		}
		return chars;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of ints for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static int[] asIntArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		int[] ints = new int[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof IntValue intValue && intValue.hasKnownValue())
				ints[i] = asInt(intValue);
		}
		return ints;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of floats for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static float[] asFloatArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		float[] floats = new float[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof FloatValue floatValue && floatValue.hasKnownValue())
				floats[i] = asFloat(floatValue);
		}
		return floats;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of doubles for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static double[] asDoubleArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		double[] doubles = new double[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof DoubleValue doubleValue && doubleValue.hasKnownValue())
				doubles[i] = asDouble(doubleValue);
		}
		return doubles;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of longs for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static long[] asLongArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		long[] longs = new long[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof LongValue longValue && longValue.hasKnownValue())
				longs[i] = asLong(longValue);
		}
		return longs;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of strings for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static String[] asStringArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		String[] strings = new String[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof ObjectValue objectValue && objectValue.hasKnownValue())
				strings[i] = asStringOrNull(objectValue);
		}
		return strings;
	}

	/**
	 * @param value
	 * 		Value of an array to convert.
	 *
	 * @return Array of objects for the given array value.
	 *
	 * @throws IllegalArgumentException
	 * 		When the array does not have a known length.
	 */
	@Nullable
	public static Object[] asObjectArray(@Nonnull ArrayValue value) {
		if (value.isNull())
			return null;
		int length = getKnownArrayLength(value);
		Object[] objects = new Object[length];
		for (int i = 0; i < length; i++) {
			ReValue element = value.getValue(i);
			if (element instanceof ObjectValue objectValue && objectValue.hasKnownValue())
				objects[i] = asObject(objectValue);
		}
		return objects;
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toBooleanArrayValue(@Nullable boolean[] value) {
		if (value == null)
			return ArrayValue.VAL_BOOLEANS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_BOOLEAN, Nullness.NOT_NULL, value.length, index -> toBooleanValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toByteArrayValue(@Nullable byte[] value) {
		if (value == null)
			return ArrayValue.VAL_BYTES_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_BYTE, Nullness.NOT_NULL, value.length, index -> toByteValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toShortArrayValue(@Nullable short[] value) {
		if (value == null)
			return ArrayValue.VAL_SHORTS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_SHORT, Nullness.NOT_NULL, value.length, index -> toShortValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toCharArrayValue(@Nullable char[] value) {
		if (value == null)
			return ArrayValue.VAL_CHARS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_CHAR, Nullness.NOT_NULL, value.length, index -> toCharValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toIntArrayValue(@Nullable int[] value) {
		if (value == null)
			return ArrayValue.VAL_INTS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_INT, Nullness.NOT_NULL, value.length, index -> toIntValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toLongArrayValue(@Nullable long[] value) {
		if (value == null)
			return ArrayValue.VAL_LONGS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_LONG, Nullness.NOT_NULL, value.length, index -> toLongValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toFloatArrayValue(@Nullable float[] value) {
		if (value == null)
			return ArrayValue.VAL_FLOATS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_FLOAT, Nullness.NOT_NULL, value.length, index -> toFloatValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toDoubleArrayValue(@Nullable double[] value) {
		if (value == null)
			return ArrayValue.VAL_DOUBLES_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_DOUBLE, Nullness.NOT_NULL, value.length, index -> toDoubleValue(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toCharSequenceArrayValue(@Nullable CharSequence[] value) {
		if (value == null)
			return ArrayValue.VAL_STRINGS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.NOT_NULL, value.length, index -> ObjectValue.string(String.valueOf(value[index])));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toStringArrayValue(@Nullable String[] value) {
		if (value == null)
			return ArrayValue.VAL_STRINGS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.NOT_NULL, value.length, index -> ObjectValue.string(value[index]));
	}

	/**
	 * @param value
	 * 		Array to convert.
	 *
	 * @return Wrapped array representation of the value.
	 */
	@Nonnull
	public static ArrayValue toObjectArrayValue(@Nullable Object[] value) {
		if (value == null)
			return ArrayValue.VAL_OBJECTS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_OBJECT, Nullness.NOT_NULL, value.length, index -> toObjectValue(value[index]));
	}

	private static int getKnownArrayLength(@Nonnull ArrayValue value) {
		OptionalInt length = value.getFirstDimensionLength();
		if (length.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException("Array value does not have known length");
		return length.getAsInt();
	}

	@Nonnull
	private static <T> T hostArray(@Nonnull ReValue value, @Nonnull Class<T> type) {
		if (value instanceof InstancedObjectValue<?> instanced) {
			Object realInstance = instanced.getRealInstance();
			if (type.isInstance(realInstance))
				return type.cast(realInstance);
		}
		throw new IllegalArgumentException("Expected host-backed " + type.getTypeName() + ": " + value);
	}
}
