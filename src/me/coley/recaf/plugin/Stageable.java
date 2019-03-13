package me.coley.recaf.plugin;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import me.coley.recaf.ui.component.ActionMenuItem;
import me.coley.recaf.util.JavaFX;

/**
 * Interface applied to plugins that will be shown in the menu-bar under the
 * "plugins" menu.
 * 
 * @author Matt
 */
public interface Stageable {
	/**
	 * @return MenuItem to be added to the menu-bar entry for plugins.
	 */
	default MenuItem createMenuItem() {
		return new ActionMenuItem(title(), () -> stage().show());
	}

	/**
	 * @return Content of stage to create.
	 */
	Parent content();

	/**
	 * @return Title of stage to create.
	 */
	String title();

	/**
	 * @return Width of stage to create.
	 */
	int width();

	/**
	 * @return Height of stage to create.
	 */
	int height();

	/**
	 * @return Whether or not the plugin window should be top-most.
	 */
	default boolean isTopmost() {
		return false;
	}

	/**
	 * @return New scene with {@link #content()} sized to {@link #width()} and
	 *         {@link #height()}.
	 */
	default Scene scene() {
		return JavaFX.scene(content(), width(), height());
	}

	/**
	 * @return New stage from {@link #scene()}.
	 */
	default Stage stage() {
		return JavaFX.stage(scene(), title(), isTopmost());
	}
}
