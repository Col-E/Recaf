package software.coley.recaf.ui.pane.editing.hex.ops;

/**
 * Outlines the data access model for the hex editor.
 *
 * @author Matt Coley
 */
public interface HexAccess {
	/**
	 * @return Data to operate on.
	 */
	byte[] getData();

	/**
	 * @return Length of the data.
	 */
	default int length() {
		return getData().length;
	}

	/**
	 * @param offset
	 * 		Offset into the data.
	 *
	 * @return Value at offset. Any out of bounds value is mapped to {@code 0}.
	 */
	default byte getByte(int offset) {
		if (isInBounds(offset))
			return getData()[offset];
		return 0;
	}

	/**
	 * @param offset
	 * 		Offset into the data.
	 *
	 * @return {@code true} when the offset is within the data size.
	 */
	default boolean isInBounds(int offset) {
		return offset >= 0 && offset < length();
	}

	/**
	 * Update the {@link #getData() data} model with the given value at the given offset.
	 * Does nothing if the offset is outside the data bounds.
	 *
	 * @param offset
	 * 		Offset into the data.
	 * @param b
	 * 		Value to set.
	 */
	default void setByte(int offset, byte b) {
		if (isInBounds(offset)) getData()[offset] = b;
	}
}
