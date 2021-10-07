package me.coley.recaf.ui.control.hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for managing a selection range. To retrieve the selection range see:
 * <ul>
 *     <li>{@link #getStart()}</li>
 *     <li>{@link #getEnd()}</li>
 * </ul>
 * And for range updates:
 * <ul>
 *     <li>{@link #addListener(HexRangeListener)}</li>
 *     <li>{@link #removeListener(HexRangeListener)}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class HexRange {
	private final List<HexRangeListener> listeners = new ArrayList<>();
	private final HexAccessor hex;
	// These are NOT meant to be the actual min/max values.
	// The start is the index the user clicks first.
	// The end is the index the user last dragged over.
	private int start = -1;
	private int end = -1;

	/**
	 * @param hex
	 * 		Associated hex data accessor.
	 */
	public HexRange(HexAccessor hex) {
		this.hex = hex;
	}

	/**
	 * Clears current selection.
	 */
	public void clearSelection() {
		int oldStart = getStart();
		int oldEnd = getEnd();
		start = -1;
		end = -1;
		listeners.forEach(l -> l.onSelectionClear(oldStart, oldEnd));
	}

	/**
	 * Called when the user first begins a drag <i>(multi-select)</i> operation.
	 *
	 * @param offset
	 * 		Hex offset.
	 */
	public void createSelectionBound(int offset) {
		clearSelection();
		offset = cap(offset);
		start = offset;
	}

	/**
	 * Called when the user updates a drag <i>(multi-select)</i> operation.
	 *
	 * @param offset
	 * 		Hex offset.
	 */
	public void updateSelectionBound(int offset) {
		if (start > -1) {
			offset = cap(offset);
			end = offset;
			listeners.forEach(l -> l.onSelectionUpdate(getStart(), getEnd()));
		}
	}

	/**
	 * Called when the user first completes a drag <i>(multi-select)</i> operation.
	 */
	public void endSelectionBound() {
		if (start > -1 && end == -1) {
			end = start;
		}
		if (exists()) {
			listeners.forEach(l -> l.onSelectionComplete(getStart(), getEnd()));
		}
	}

	/**
	 * @return {@code true} when there is a current selection.
	 * {@code false} means no selection is made.
	 */
	public boolean exists() {
		return start > -1 && end > -1;
	}

	/**
	 * @return {@code true} for a single cell selection.
	 */
	public boolean isSingle() {
		return exists() && start == end;
	}

	/**
	 * @return {@code true} for multiple cell selection.
	 */
	public boolean isMulti() {
		return exists() && !isSingle();
	}

	/**
	 * @param offset
	 * 		Offset to check.
	 *
	 * @return {@code true} when the offset is within the selection range.
	 */
	public boolean isInRange(int offset) {
		if (!exists())
			return false;
		return offset >= getStart() && offset <= getEnd();
	}

	/**
	 * @return Minimum bound of selection.
	 */
	public int getStart() {
		return Math.min(start, end);
	}

	/**
	 * @return Maximnum bound of selection.
	 */
	public int getEnd() {
		return Math.max(start, end);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addListener(HexRangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public boolean removeListener(HexRangeListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Caps offset off at hex data length.
	 *
	 * @param offset
	 * 		Some offset.
	 *
	 * @return Offset, or data length cap.
	 */
	private int cap(int offset) {
		return Math.min(offset, hex.getLength() - 1);
	}
}
