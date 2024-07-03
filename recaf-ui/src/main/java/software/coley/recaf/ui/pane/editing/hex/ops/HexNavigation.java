package software.coley.recaf.ui.pane.editing.hex.ops;

/**
 * Outlines UI navigation for the hex editor.
 *
 * @author Matt Coley
 */
public interface HexNavigation {
	/**
	 * @return Current offset in the data that is selected.
	 */
	int selectionOffset();

	/**
	 * The hex editor has two main columns for editing data. The same offset is used for both, but determining
	 * which one is targeted in the UI is determined here.
	 *
	 * @return {@code true} if the selection is in a hex column.
	 * {@code false} if the selection is in an ascii column.
	 *
	 * @see #switchColumns() For changing which column is selected.
	 */
	boolean isHexColumnSelected();

	/**
	 * Switches which column is selected, between hex and ascii.
	 */
	void switchColumns();

	/**
	 * Selects the given offset in the data. The value is clamped to fit if it is out of bounds.
	 *
	 * @param offset
	 * 		Offset in the data to select.
	 */
	void select(int offset);

	/**
	 * Move selection by {@code +1}.
	 */
	void selectNext();

	/**
	 * Move selection by {@code -1}.
	 */
	void selectPrevious();

	/**
	 * Move selection by {@code +R} where:
	 * <ul>
	 *     <li>{@code R} is the number of values shown per row.</li>
	 * </ul>
	 */
	void selectDown();

	/**
	 * Move selection by {@code -R} where:
	 * <ul>
	 *     <li>{@code R} is the number of values shown per row.</li>
	 * </ul>
	 */
	void selectUp();

	/**
	 * Move selection by {@code +N*R} where {@code R} is the number of values shown per row.
	 * <ul>
	 *     <li>{@code R} is the number of values shown per row.</li>
	 *     <li>{@code N} is the number of rows</li>
	 * </ul>
	 */
	void selectDown(int rows);

	/**
	 * Move selection by {@code -N*R} where:
	 * <ul>
	 *     <li>{@code R} is the number of values shown per row.</li>
	 *     <li>{@code N} is the number of rows</li>
	 * </ul>
	 */
	void selectUp(int rows);
}
