package me.coley.recaf.ui.behavior;

/**
 * Snapshot of scroll position.
 *
 * @author Matt Coley
 * @see Scrollable
 */
public interface ScrollSnapshot {
	/**
	 * Restore the recorded positions.
	 */
	void restore();
}
