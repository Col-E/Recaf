package me.coley.recaf.ui.control;

import javafx.scene.Node;
import javafx.scene.control.Menu;

/**
 * A non-interactive menu implementation for display purposes only.
 *
 * @author Matt Coley
 */
public class MenuLabel extends Menu {
	/**
	 * @param text
	 * 		Text to display
	 */
	public MenuLabel(String text) {
		this(text, null);
	}

	/**
	 * @param graphic
	 * 		Graphic to display
	 */
	public MenuLabel(Node graphic) {
		this(null, graphic);
	}

	/**
	 * @param text
	 * 		Text to display
	 * @param graphic
	 * 		Graphic to display
	 */
	public MenuLabel(String text, Node graphic) {
		setText(text);
		setGraphic(graphic);
		// Prevent background color change on hover
		setStyle("-fx-background-insets: 100; -fx-font-style: italic;");
	}
}
