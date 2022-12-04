package me.coley.recaf.ui.behavior;

import javafx.scene.Node;
import me.coley.recaf.workspace.WorkspaceCloseListener;

/**
 * Children of this type represent some item, and thus should provide access to the
 * UI implementation of the representation.
 *
 * @author Matt Coley
 */
public interface Representation extends WorkspaceCloseListener {
	/**
	 * Called when the "save" keybind is pressed.
	 * The current representation of the class/file is updated in the primary resource.
	 *
	 * @return Result type for save operation.
	 */
	SaveResult save();

	/**
	 * Some representations may be read-only.
	 * For those that support editing, the save keybind will apply any changes made.
	 *
	 * @return {@code true} when the current representation supports editing.
	 */
	boolean supportsEditing();

	/**
	 * @return UI element of the representation.
	 */
	Node getNodeRepresentation();
}
