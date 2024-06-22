package software.coley.recaf.ui.pane.editing.hex.cell;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;

/**
 * Outline of a hex-cell which displays the {@code byte} of data at a given {@link #offset()}.
 *
 * @author Matt Coley
 */
public interface HexCell {
	/**
	 * @return Cell display node.
	 */
	@Nonnull
	Node node();

	/**
	 * @return Offset into the data this cell represents.
	 */
	int offset();

	/**
	 * Handle selection gain event.
	 */
	void onSelectionGained();

	/**
	 * Handle selection lost event.
	 */
	void onSelectionLost();

	/**
	 * @return {@code true} when the cell is in edit mode.
	 * {@code false} when the cell is in read-only display mode.
	 */
	boolean isEditing();

	/**
	 * Sets the cell to edit mode.
	 *
	 * @see #isEditing()
	 */
	void beginEdit();

	/**
	 * Sets the cell to read-only display mode, committing any edits made if desired.
	 *
	 * @param commit
	 *        {@code true} to commit changes to the data.
	 *        {@code false} to cancel any edits made in this cell.
	 */
	void endEdit(boolean commit);

	/**
	 * Used to accept overflow inputs from adjacent cells.
	 * When typing in one cell is done, the next key press is sent to the next cell <i>(if there is one)</i>.
	 *
	 * @param code
	 * 		Key code to handle as user input.
	 */
	void handleKeyCode(@Nonnull KeyCode code);
}
