package me.coley.recaf.plugin.api;

import javafx.scene.control.Menu;

/**
 * Allow plugins to insert themselves into the plugin menu dropdown.
 *
 * @author Matt
 */
public interface MenuProviderPlugin extends BasePlugin {
	/**
	 * Create a menu/menu-item for the plugin.
	 *
	 * @return Generated menu.
	 */
	Menu createMenu();
}
