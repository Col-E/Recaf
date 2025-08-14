package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * Value capable of recording partial details of array content.
 *
 * @author Matt Coley
 */
public interface ArrayValue extends ObjectValue {
	ArrayValue VAL_BOOLEANS = new ArrayValueImpl(Type.getType("[Z"), Nullness.NOT_NULL);
	ArrayValue VAL_BOOLEANS_NULL = new ArrayValueImpl(Type.getType("[Z"), Nullness.NULL);
	ArrayValue VAL_CHARS = new ArrayValueImpl(Type.getType("[C"), Nullness.NOT_NULL);
	ArrayValue VAL_CHARS_NULL = new ArrayValueImpl(Type.getType("[C"), Nullness.NULL);
	ArrayValue VAL_BYTES = new ArrayValueImpl(Type.getType("[B"), Nullness.NOT_NULL);
	ArrayValue VAL_BYTES_NULL = new ArrayValueImpl(Type.getType("[B"), Nullness.NULL);
	ArrayValue VAL_SHORTS = new ArrayValueImpl(Type.getType("[S"), Nullness.NOT_NULL);
	ArrayValue VAL_SHORTS_NULL = new ArrayValueImpl(Type.getType("[S"), Nullness.NULL);
	ArrayValue VAL_INTS = new ArrayValueImpl(Type.getType("[I"), Nullness.NOT_NULL);
	ArrayValue VAL_INTS_NULL = new ArrayValueImpl(Type.getType("[I"), Nullness.NULL);
	ArrayValue VAL_FLOATS = new ArrayValueImpl(Type.getType("[F"), Nullness.NOT_NULL);
	ArrayValue VAL_FLOATS_NULL = new ArrayValueImpl(Type.getType("[F"), Nullness.NULL);
	ArrayValue VAL_DOUBLES = new ArrayValueImpl(Type.getType("[D"), Nullness.NOT_NULL);
	ArrayValue VAL_DOUBLES_NULL = new ArrayValueImpl(Type.getType("[D"), Nullness.NULL);
	ArrayValue VAL_LONGS = new ArrayValueImpl(Type.getType("[J"), Nullness.NOT_NULL);
	ArrayValue VAL_LONGS_NULL = new ArrayValueImpl(Type.getType("[J"), Nullness.NULL);
	ArrayValue VAL_OBJECTS = new ArrayValueImpl(Type.getType("[Ljava/lang/Object;"), Nullness.NOT_NULL);
	ArrayValue VAL_OBJECTS_NULL = new ArrayValueImpl(Type.getType("[Ljava/lang/Object;"), Nullness.NULL);
	ArrayValue VAL_STRINGS = new ArrayValueImpl(Type.getType("[Ljava/lang/String;"), Nullness.NOT_NULL);
	ArrayValue VAL_STRINGS_NULL = new ArrayValueImpl(Type.getType("[Ljava/lang/String;"), Nullness.NULL);

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
			case "[Z" -> nullness == Nullness.NULL ? VAL_BOOLEANS_NULL : VAL_BOOLEANS;
			case "[C" -> nullness == Nullness.NULL ? VAL_CHARS_NULL : VAL_CHARS;
			case "[B" -> nullness == Nullness.NULL ? VAL_BYTES_NULL : VAL_BYTES;
			case "[S" -> nullness == Nullness.NULL ? VAL_SHORTS_NULL : VAL_SHORTS;
			case "[I" -> nullness == Nullness.NULL ? VAL_INTS_NULL : VAL_INTS;
			case "[F" -> nullness == Nullness.NULL ? VAL_FLOATS_NULL : VAL_FLOATS;
			case "[D" -> nullness == Nullness.NULL ? VAL_DOUBLES_NULL : VAL_DOUBLES;
			case "[J" -> nullness == Nullness.NULL ? VAL_LONGS_NULL : VAL_LONGS;
			case "[Ljava/lang/String;" -> nullness == Nullness.NULL ? VAL_STRINGS_NULL : VAL_STRINGS;
			case "[Ljava/lang/Object;" -> nullness == Nullness.NULL ? VAL_OBJECTS_NULL : VAL_OBJECTS;
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
	 * @return Value, if known, at the given index. Otherwise, a {@link ReValue} of the array's element type..
	 */
	@Nullable
	ReValue getValue(int index);
}
