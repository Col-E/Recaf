package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Base window attributes.
 *
 * @author Matt Coley
 */
public abstract class WindowBase extends Stage {
	/**
	 * @return Stage scene with prepared content.
	 */
	protected abstract Scene createScene();
}
