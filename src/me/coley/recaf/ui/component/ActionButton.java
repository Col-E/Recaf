package me.coley.recaf.ui.component;

import javafx.scene.control.Button;

/**
 * On-click action button. Ease-of-access inlining.
 * 
 * @author Matt
 */
public final class ActionButton extends Button {
	public ActionButton(String text, Runnable action) {
		super(text);
		setOnAction(o -> action.run());
	}
}