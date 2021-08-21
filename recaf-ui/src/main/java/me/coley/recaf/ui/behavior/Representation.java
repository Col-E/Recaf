package me.coley.recaf.ui.behavior;

import javafx.scene.Node;

/**
 * Children of this type represent some item, and thus should provide access to the UI implementation of the representation.
 *
 * @author Matt Coley
 */
public interface Representation {
	/**
	 * @return UI element of the representation.
	 */
	Node getNodeRepresentation();
}
