package me.coley.recaf.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/**
 * MenuItem with an on-click runnable action.
 *
 * @author Matt
 */
public class ActionMenuItem extends MenuItem {
	/**
	 * @param text
	 * 		Menu item display text.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionMenuItem(String text, Runnable action) {
		this(text, null, action);
	}

	/**
	 * @param text
	 * 		Menu item display text.
	 * @param graphic
	 * 		Menu item graphic.
	 * @param action
	 * 		Action to run on-click.
	 */
	public ActionMenuItem(String text, Node graphic, Runnable action) {
		super(text);
		setGraphic(graphic);
		setOnAction(e -> action.run());
	}
}
