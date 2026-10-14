package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.IntRange;

/**
 * Directed selection between byte boundaries. The selected range is half-open, so its end is exclusive.
 *
 * @param anchor
 * 		Fixed boundary of the selection.
 * @param caret
 * 		Moving boundary of the selection.
 *
 * @author Matt Coley
 */
public record HexSelection(int anchor, int caret) {
	/**
	 * @return Unordered half-open byte range represented by this selection.
	 */
	@Nonnull
	public IntRange range() {
		return new IntRange(Math.min(anchor, caret), Math.max(anchor, caret));
	}
}
