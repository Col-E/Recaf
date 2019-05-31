package me.coley.recaf.ui.component;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/**
 * MenuItem with action set in the constructor.
 * 
 * @author Matt
 */
public class ActionMenuItem extends MenuItem {
	public ActionMenuItem(String text, Runnable action) {
		super(text);
		setOnAction(o -> action.run());
	}

	public ActionMenuItem(String text, Node graphic, Runnable action) {
		super(text);
		setGraphic(graphic);
		setOnAction(o -> action.run());
	}
}