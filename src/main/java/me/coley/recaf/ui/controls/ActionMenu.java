package me.coley.recaf.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.HBox;

/**
 * Menu with an on-click runnable action.
 *
 * @author Matt
 */
public class ActionMenu extends Menu {
	/**
	 * @param text
	 * 		Menu item display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionMenu(String text, Runnable action) {
		this(text, null, action);
	}

	/**
	 * @param text
	 * 		Menu display text.
	 * @param graphic
	 * 		Menu graphic.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionMenu(String text, Node graphic, Runnable action) {
		super();
		// This is a hack: https://stackoverflow.com/a/10317260
		// Works well enough without having to screw with CSS.
		Label label = new Label(text);
		label.getStyleClass().add("action-menu-title");
		if (graphic != null)
			setGraphic(new HBox(graphic, label));
		else
			setGraphic(label);
		label.setOnMousePressed(e -> action.run());
	}
}
