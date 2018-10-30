package me.coley.recaf.ui.component;

import javafx.scene.control.Label;
import javafx.scene.control.Menu;

/**
 * Menu <i>(Has no children)</i> that runs an action when clicked.
 * 
 * @author Matt
 */
public class ActionMenu extends Menu {
	public ActionMenu(String text, Runnable action) {
		// This is a hack: https://stackoverflow.com/a/10317260
		// Works well enough without having to screw with CSS.
		Label label = new Label(text);
		setGraphic(label);
		label.setOnMouseClicked(o -> action.run());
	}
}