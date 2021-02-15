package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Base window attributes.
 *
 * @author Matt Coley
 */
public abstract class WindowBase extends Stage {
	/**
	 * Create the scene and add the base stylesheets.
	 */
	protected void init() {
		setScene(createScene());
		getScene().getStylesheets().add("style/base.css");
		getScene().getStylesheets().add("style/scroll.css");
		getScene().getStylesheets().add("style/tree.css");
		getIcons().add(new Image("icons/logo.png"));
	}

	/**
	 * @return Stage scene with prepared content.
	 */
	protected abstract Scene createScene();
}
