package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

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
		addStylesheets(getScene().getStylesheets());
		getIcons().add(new Image("icons/logo.png"));
	}

	/**
	 * @return Stage scene with prepared content.
	 */
	protected abstract Scene createScene();

	/**
	 * @param stylesheets
	 * 		Stylesheet list to update.
	 */
	public static void addStylesheets(List<String> stylesheets) {
		stylesheets.addAll(Arrays.asList("style/base.css",
				"style/code.css",
				"style/log.css",
				"style/scroll.css",
				"style/tabs.css",
				"style/text.css",
				"style/tree.css",
				"style/split.css")
		);
	}
}
