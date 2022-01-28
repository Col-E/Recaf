package me.coley.recaf.ui.control;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;

/**
 * Button with an on-click runnable action.
 *
 * @author Matt Coley
 */
public final class ActionButton extends Button {
	/**
	 * @param text
	 * 		Button display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionButton(String text, Runnable action) {
		super(text);
		setOnAction(o -> action.run());
	}

	/**
	 * @param text
	 * 		Button display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionButton(ObservableValue<String> text, Runnable action) {
		textProperty().bind(text);
		setOnAction(o -> action.run());
	}
}