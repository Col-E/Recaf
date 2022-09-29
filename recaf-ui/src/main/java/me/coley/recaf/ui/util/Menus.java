package me.coley.recaf.ui.util;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import me.coley.recaf.ui.control.menu.ActionMenu;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.util.TextDisplayUtil;

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
		MenuItem header = new MenuItem(TextDisplayUtil.shortenEscapeLimit(name));
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
	 * @return Menu instance.
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
	 * @return Menu instance, with optional graphic.
	 */
	public static Menu menu(String textKey, String imagePath) {
		return menu(textKey, imagePath, false);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param antialias
	 * 		Flag to enable anti-aliasing of the image graphic.
	 *
	 * @return Menu instance, with optional graphic.
	 */
	public static Menu menu(String textKey, String imagePath, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		Menu menu = new Menu();
		menu.textProperty().bind(Lang.getBinding(textKey));
		menu.setGraphic(graphic);
		return menu;
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Menu instance, with behavior on-click.
	 */
	public static Menu actionMenu(String textKey, String imagePath, Runnable runnable) {
		return actionMenu(textKey, imagePath, runnable, false);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 * @param antialias
	 * 		Flag to enable anti-aliasing of the image graphic.
	 *
	 * @return Menu instance, with behavior on-click.
	 */
	public static Menu actionMenu(String textKey, String imagePath, Runnable runnable, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		return new ActionMenu(Lang.getBinding(textKey), graphic, runnable);
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
	public static ActionMenuItem action(String textKey, Runnable runnable) {
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
		return action(textKey, imagePath, runnable, false);
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
	 * @param antialias
	 * 		Flag to enable anti-aliasing of the image graphic.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	public static ActionMenuItem action(String textKey, String imagePath, Runnable runnable, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		return new ActionMenuItem(Lang.getBinding(textKey), graphic, runnable);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link ActionMenuItem}s.
	 *
	 * @param text
	 * 		Menu item text.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	public static ActionMenuItem actionLiteral(String text, String imagePath, Runnable runnable) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new ActionMenuItem(text, graphic, runnable);
	}

	/**
	 * @return New menu separator.
	 */
	public static SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}
