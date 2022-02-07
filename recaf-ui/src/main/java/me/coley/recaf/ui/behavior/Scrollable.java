package me.coley.recaf.ui.behavior;

/**
 * Outline of a scrollable component.
 *
 * @author Matt Coley
 */
public interface Scrollable {
	/**
	 * @return Snapshot of scroll position.
	 */
	ScrollSnapshot makeScrollSnapshot();
}
