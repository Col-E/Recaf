package me.coley.recaf.ui.control.hex;

/**
 * Listener for range updates of {@link HexRange}.
 * The start and stop values are always ordered such that {@code start < stop}.
 *
 * @author Matt Coley
 */
public interface HexRangeListener {
	/**
	 * Called when selection has been updated.
	 *
	 * @param start
	 * 		Min bound.
	 * @param stop
	 * 		Max bound.
	 */
	void onSelectionUpdate(int start, int stop);

	/**
	 * Called when selection has been completed <i>(Mouse release)</i>.
	 *
	 * @param start
	 * 		Min bound.
	 * @param stop
	 * 		Max bound.
	 */
	void onSelectionComplete(int start, int stop);

	/**
	 * Called when selection has been cleared.
	 *
	 * @param start
	 * 		Min bound.
	 * @param stop
	 * 		Max bound.
	 */
	void onSelectionClear(int start, int stop);
}
