package me.coley.recaf.ui.behavior;

/**
 * Children of this type represent some item and a view state that supports loading a prior version of
 * the represented item.
 *
 * @author Matt Coley
 */
public interface Undoable {
	/**
	 * Called when the "undo" keybind is pressed.
	 * The last change to the current class/file is discarded.
	 */
	void undo();
}
