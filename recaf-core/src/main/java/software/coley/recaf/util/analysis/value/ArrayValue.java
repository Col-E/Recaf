package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;

import java.util.OptionalInt;

/**
 * Value capable of recording partial details of array content.
 *
 * @author Matt Coley
 */
public interface ArrayValue extends ObjectValue {
	ArrayValue VAL_BOOLEANS = new ArrayValueImpl(Type.getType("[Z"), Nullness.NOT_NULL);
	ArrayValue VAL_CHARS = new ArrayValueImpl(Type.getType("[C"), Nullness.NOT_NULL);
	ArrayValue VAL_BYTES = new ArrayValueImpl(Type.getType("[B"), Nullness.NOT_NULL);
	ArrayValue VAL_SHORTS = new ArrayValueImpl(Type.getType("[S"), Nullness.NOT_NULL);
	ArrayValue VAL_INTS = new ArrayValueImpl(Type.getType("[I"), Nullness.NOT_NULL);
	ArrayValue VAL_FLOATS = new ArrayValueImpl(Type.getType("[F"), Nullness.NOT_NULL);
	ArrayValue VAL_DOUBLES = new ArrayValueImpl(Type.getType("[D"), Nullness.NOT_NULL);
	ArrayValue VAL_LONGS = new ArrayValueImpl(Type.getType("[J"), Nullness.NOT_NULL);

	/**
	 * @param type
	 * 		Array type.
	 * @param nullness
	 * 		Array null state.
	 *
	 * @return Array value holding the array content.
	 */
	@Nonnull
	static ArrayValue of(@Nonnull Type type, @Nonnull Nullness nullness) {
		String descriptor = type.getDescriptor();
		return switch (descriptor) {
			case "[Z" -> VAL_BOOLEANS;
			case "[C" -> VAL_CHARS;
			case "[B" -> VAL_BYTES;
			case "[S" -> VAL_SHORTS;
			case "[I" -> VAL_INTS;
			case "[F" -> VAL_FLOATS;
			case "[D" -> VAL_DOUBLES;
			case "[J" -> VAL_LONGS;
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
	 * @return Array value holding the array content.
	 */
	@Nonnull
	static ArrayValue of(@Nonnull Type type, @Nonnull Nullness nullness, int length) {
		return new ArrayValueImpl(type, nullness, length);
	}

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
}
