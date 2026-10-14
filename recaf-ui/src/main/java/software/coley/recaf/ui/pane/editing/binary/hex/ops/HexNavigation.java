package software.coley.recaf.ui.pane.editing.binary.hex.ops;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.pane.editing.binary.hex.HexSelection;

/**
 * Outlines UI navigation for the hex editor.
 *
 * @author Matt Coley
 */
public interface HexNavigation {
	/**
	 * @return Current directed selection. Its anchor and caret are byte boundaries, including the end of the data.
	 */
	@Nonnull
	HexSelection selection();

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
	 * Select a single byte at the given offset. Clamped to {@code [0, data length]}.
	 *
	 * @param offset
	 * 		Byte offset to select.
	 */
	void select(int offset);

	/**
	 * Select a range of bytes. Both boundaries are clamped to {@code [0, data length]}.
	 *
	 * @param anchor
	 * 		Fixed boundary.
	 * @param caret
	 * 		Moving boundary.
	 */
	void selectRange(int anchor, int caret);

	/**
	 * Move the caret to a byte boundary, optionally extending from the current anchor.
	 *
	 * @param offset
	 * 		Target boundary.
	 * @param extend
	 *        {@code true} to preserve the current anchor.
	 */
	void moveTo(int offset, boolean extend);
}
