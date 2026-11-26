package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * Value capable of recording partial details of array content.
 *
 * @author Matt Coley
 */
public interface ArrayValue extends ObjectValue {
	ArrayValue VAL_BOOLEANS = new ArrayValueImpl(Types.ARRAY_1D_BOOLEAN, Nullness.NOT_NULL);
	ArrayValue VAL_BOOLEANS_NULL = new ArrayValueImpl(Types.ARRAY_1D_BOOLEAN, Nullness.NULL);
	ArrayValue VAL_BOOLEANS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_BOOLEAN, Nullness.UNKNOWN);
	ArrayValue VAL_CHARS = new ArrayValueImpl(Types.ARRAY_1D_CHAR, Nullness.NOT_NULL);
	ArrayValue VAL_CHARS_NULL = new ArrayValueImpl(Types.ARRAY_1D_CHAR, Nullness.NULL);
	ArrayValue VAL_CHARS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_CHAR, Nullness.UNKNOWN);
	ArrayValue VAL_BYTES = new ArrayValueImpl(Types.ARRAY_1D_BYTE, Nullness.NOT_NULL);
	ArrayValue VAL_BYTES_NULL = new ArrayValueImpl(Types.ARRAY_1D_BYTE, Nullness.NULL);
	ArrayValue VAL_BYTES_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_BYTE, Nullness.UNKNOWN);
	ArrayValue VAL_SHORTS = new ArrayValueImpl(Types.ARRAY_1D_SHORT, Nullness.NOT_NULL);
	ArrayValue VAL_SHORTS_NULL = new ArrayValueImpl(Types.ARRAY_1D_SHORT, Nullness.NULL);
	ArrayValue VAL_SHORTS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_SHORT, Nullness.UNKNOWN);
	ArrayValue VAL_INTS = new ArrayValueImpl(Types.ARRAY_1D_INT, Nullness.NOT_NULL);
	ArrayValue VAL_INTS_NULL = new ArrayValueImpl(Types.ARRAY_1D_INT, Nullness.NULL);
	ArrayValue VAL_INTS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_INT, Nullness.UNKNOWN);
	ArrayValue VAL_FLOATS = new ArrayValueImpl(Types.ARRAY_1D_FLOAT, Nullness.NOT_NULL);
	ArrayValue VAL_FLOATS_NULL = new ArrayValueImpl(Types.ARRAY_1D_FLOAT, Nullness.NULL);
	ArrayValue VAL_FLOATS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_FLOAT, Nullness.UNKNOWN);
	ArrayValue VAL_DOUBLES = new ArrayValueImpl(Types.ARRAY_1D_DOUBLE, Nullness.NOT_NULL);
	ArrayValue VAL_DOUBLES_NULL = new ArrayValueImpl(Types.ARRAY_1D_DOUBLE, Nullness.NULL);
	ArrayValue VAL_DOUBLES_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_DOUBLE, Nullness.UNKNOWN);
	ArrayValue VAL_LONGS = new ArrayValueImpl(Types.ARRAY_1D_LONG, Nullness.NOT_NULL);
	ArrayValue VAL_LONGS_NULL = new ArrayValueImpl(Types.ARRAY_1D_LONG, Nullness.NULL);
	ArrayValue VAL_LONGS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_LONG, Nullness.UNKNOWN);
	ArrayValue VAL_OBJECTS = new ArrayValueImpl(Types.ARRAY_1D_OBJECT, Nullness.NOT_NULL);
	ArrayValue VAL_OBJECTS_NULL = new ArrayValueImpl(Types.ARRAY_1D_OBJECT, Nullness.NULL);
	ArrayValue VAL_OBJECTS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_OBJECT, Nullness.UNKNOWN);
	ArrayValue VAL_STRINGS = new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.NOT_NULL);
	ArrayValue VAL_STRINGS_NULL = new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.NULL);
	ArrayValue VAL_STRINGS_MAYBE_NULL = new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.UNKNOWN);

	/**
	 * @param type
	 * 		Array type.
	 * @param nullness
	 * 		Array null state.
	 *
	 * @return Array value of the given type.
	 */
	@Nonnull
	static ArrayValue of(@Nonnull Type type, @Nonnull Nullness nullness) {
		String descriptor = type.getDescriptor();
		return switch (descriptor) {
			case "[Z" -> nullness == Nullness.NULL ? VAL_BOOLEANS_NULL : nullness == Nullness.UNKNOWN ? VAL_BOOLEANS_MAYBE_NULL : VAL_BOOLEANS;
			case "[C" -> nullness == Nullness.NULL ? VAL_CHARS_NULL : nullness == Nullness.UNKNOWN ? VAL_CHARS_MAYBE_NULL : VAL_CHARS;
			case "[B" -> nullness == Nullness.NULL ? VAL_BYTES_NULL : nullness == Nullness.UNKNOWN ? VAL_BYTES_MAYBE_NULL : VAL_BYTES;
			case "[S" -> nullness == Nullness.NULL ? VAL_SHORTS_NULL : nullness == Nullness.UNKNOWN ? VAL_SHORTS_MAYBE_NULL : VAL_SHORTS;
			case "[I" -> nullness == Nullness.NULL ? VAL_INTS_NULL : nullness == Nullness.UNKNOWN ? VAL_INTS_MAYBE_NULL : VAL_INTS;
			case "[F" -> nullness == Nullness.NULL ? VAL_FLOATS_NULL : nullness == Nullness.UNKNOWN ? VAL_FLOATS_MAYBE_NULL : VAL_FLOATS;
			case "[D" -> nullness == Nullness.NULL ? VAL_DOUBLES_NULL : nullness == Nullness.UNKNOWN ? VAL_DOUBLES_MAYBE_NULL : VAL_DOUBLES;
			case "[J" -> nullness == Nullness.NULL ? VAL_LONGS_NULL : nullness == Nullness.UNKNOWN ? VAL_LONGS_MAYBE_NULL : VAL_LONGS;
			case "[Ljava/lang/String;" -> nullness == Nullness.NULL ? VAL_STRINGS_NULL : nullness == Nullness.UNKNOWN ? VAL_STRINGS_MAYBE_NULL : VAL_STRINGS;
			case "[Ljava/lang/Object;" -> nullness == Nullness.NULL ? VAL_OBJECTS_NULL : nullness == Nullness.UNKNOWN ? VAL_OBJECTS_MAYBE_NULL : VAL_OBJECTS;
			default -> new ArrayValueImpl(type, nullness);
		};
	}

	/**
	 * @param type
	 * 		Array type.
	 * @param nullness
	 * 		Array null state.
	 * @param length
	 * 		Array length.
	 *
	 * @return Array value of the given type/length.
	 */
	@Nonnull
	static ArrayValue of(@Nonnull Type type, @Nonnull Nullness nullness, int length) {
		return new ArrayValueImpl(type, nullness, length);
	}

	/**
	 * @param type
	 * 		Array type.
	 * @param dimensions
	 * 		Dimensions of the array to create.
	 *
	 * @return Array value created from a {@link Opcodes#MULTIANEWARRAY} instruction.
	 */
	@Nonnull
	static ReValue multiANewArray(@Nonnull Type type, @Nonnull int[] dimensions) {
		int length = dimensions[0];
		if (dimensions.length == 1)
			return of(type, Nullness.NOT_NULL, length);
		return new ArrayValueImpl(type, Nullness.NOT_NULL, length,
				i -> multiANewArray(Types.undimension(type), Arrays.copyOfRange(dimensions, 1, dimensions.length))
		);
	}

	/**
	 * @param value
	 * 		Value to check.
	 * @param recursive
	 *        {@code true} to check for any nested arrays, {@code false} to only check the top-level array.
	 *
	 * @return {@code true} if the given value can be stored in this array, {@code false} otherwise.
	 */
	default boolean canStore(@Nonnull ReValue value, boolean recursive) {
		Type valueType = value.type();
		if (valueType == null)
			return false;
		return Types.isArrayStorable(type(), valueType, recursive);
	}

	/**
	 * This exists strictly for performance reasons. In {@link ReFrame#replaceValue(ReValue, ReValue)} frames with many
	 * arrays + nested arrays can be very slow to update. If we know that an array does not contain any nested arrays,
	 * we can skip the recursive search for the value to replace.
	 *
	 * @return {@code true} when this array contains another sub-array, {@code false} otherwise.
	 */
	boolean containsTrackedSubArray();

	/**
	 * @param index
	 * 		Index to assign value at.
	 * @param value
	 * 		Value to assign.
	 *
	 * @return New array with the given value assigned at the given index.
	 */
	@Nonnull
	ArrayValue setValue(int index, @Nonnull ReValue value);

	/**
	 * @param originalValue
	 * 		Some value.
	 * @param updatedValue
	 * 		Some updated version of the value.
	 *
	 * @return New array with the given original value replaced with the updated value.
	 */
	@Nonnull
	ArrayValue updatedCopyIfContained(@Nonnull ReValue originalValue, @Nonnull ReValue updatedValue);

	@Override
	default boolean hasKnownValue() {
		return false;
	}

	@Nonnull
	@Override
	Type type();

	/**
	 * The element type is the base of any array. Consider the following:
	 * <ul>
	 *     <li>{@code int[]}</li>
	 *     <li>{@code int[][]}</li>
	 *     <li>{@code int[][][]}</li>
	 * </ul>
	 * The element type of each is {@code int}.
	 *
	 * @return Element type of the array.
	 */
	@Nonnull
	default Type elementType() {
		return type().getElementType();
	}

	/**
	 * Consider the following:
	 * <ul>
	 *     <li>1: {@code int[]}</li>
	 *     <li>2: {@code int[][]}</li>
	 *     <li>3: {@code int[][][]}</li>
	 * </ul>
	 *
	 * @return Dimensions of the array.
	 */
	default int dimensions() {
		return type().getDimensions();
	}

	/**
	 * Consider the following:
	 * <table>
	 *     <tr><th>Length</th><th>Array definition</th></tr>
	 *     <tr><td>{@code 7}</td><td>{@code int[7]}</td></tr>
	 *     <tr><td>{@code 7}</td><td>{@code int[7][9]}</td></tr>
	 *     <tr><td>Unknown</td><td>{@code int[][9]}</td></tr>
	 *     <tr><td>Unknown</td><td>{@code int[]}</td></tr>
	 * </table>
	 *
	 * @return Length of the first dimension.
	 */
	@Nonnull
	OptionalInt getFirstDimensionLength();

	/**
	 * @param index
	 * 		Index within {@link #getFirstDimensionLength()}.
	 *
	 * @return Value, if known, at the given index. Otherwise, a {@link ReValue} of the array's element type.
	 */
	@Nullable
	ReValue getValue(int index);
}
