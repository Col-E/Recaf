package me.coley.recaf.ui.util;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.ui.control.menu.ActionMenuItem;

/**
 * Menu utilities.
 *
 * @author Matt Coley
 */
public class Menus {
	/**
	 * @param name
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 *
	 * @return Header menu item.
	 */
	public static MenuItem createHeader(String name, Node graphic) {
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(graphic);
		header.setDisable(true);
		return header;
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	public static Menu menu(String textKey) {
		return menu(textKey, null);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	public static Menu menu(String textKey, String imagePath) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new Menu(Lang.get(textKey), graphic);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link ActionMenuItem}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	protected static ActionMenuItem action(String textKey, Runnable runnable) {
		return action(textKey, null, runnable);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link ActionMenuItem}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	public static ActionMenuItem action(String textKey, String imagePath, Runnable runnable) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new ActionMenuItem(Lang.get(textKey), graphic, runnable);
	}
}
