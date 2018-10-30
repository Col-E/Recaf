package me.coley.recaf.ui.component;

import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

/**
 * Graphic-only button that runs an action when clicked.
 * 
 * @author Matt
 */
public class ToolButton extends Button {
	public ToolButton(ImageView graphic, Runnable action) {
		setGraphic(graphic);
		getStyleClass().add("toolbutton");
		setOnAction(o -> action.run());
	}
}