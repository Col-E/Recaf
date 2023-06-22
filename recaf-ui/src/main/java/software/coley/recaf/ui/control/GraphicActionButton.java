package software.coley.recaf.ui.control;

import javafx.scene.Node;

/**
 * Action button but with only a graphic.
 *
 * @author Matt Coley
 */
public class GraphicActionButton extends ActionButton {
	/**
	 * @param graphic
	 * 		Button graphic.
	 * @param action
	 * 		Action to run on-click.
	 */
	public GraphicActionButton(Node graphic, Runnable action) {
		super((String) null, action);
		setGraphic(graphic);
		getStyleClass().add("graphic-button");
	}
}
