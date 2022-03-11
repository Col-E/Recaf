package me.coley.recaf.ui.control;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * Button with an on-click runnable action.
 *
 * @author Matt Coley
 */
public class ActionButton extends Button {
	/**
	 * @param text
	 * 		Button display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionButton(String text, Runnable action) {
		super(text);
		setOnAction(e -> wrap(e, action));
	}

	/**
	 * @param text
	 * 		Button display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionButton(ObservableValue<String> text, Runnable action) {
		textProperty().bind(text);
		setOnAction(e -> wrap(e, action));
	}

	private static void wrap(ActionEvent e, Runnable action) {
		// This stops the input from 'bleeding' through to parent control handlers.
		//  - Useful for when the button is used in 'x.setGraphic(button)' scenarios
		e.consume();
		// Then run the action.
		action.run();
	}
}