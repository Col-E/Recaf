package me.coley.recaf.ui.behavior;

/**
 * Represents a UI component that can be updated with the info value it wraps is updated in the workspace.
 *
 * @param <I>
 * 		Type of info represented.
 *
 * @author Matt Coley
 */
public interface Updatable<I> {
	/**
	 * @param newValue
	 * 		New value to set.
	 */
	void onUpdate(I newValue);
}
