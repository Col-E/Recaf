package me.coley.recaf.ui.controls;

import javafx.scene.control.Button;

/**
 * Button with an on-click runnable action.
 *
 * @author Matt
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
}