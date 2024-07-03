package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.ui.control.ActionMenu;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.IconView;

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
	 * @param limit
	 * 		Text length limit.
	 *
	 * @return Header menu item.
	 */
	@Nonnull
	public static MenuItem createHeader(@Nonnull String name, @Nullable Node graphic, int limit) {
		MenuItem header = new MenuItem(TextDisplayUtil.shortenEscapeLimit(name, limit));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(graphic);
		header.setDisable(true);
		return header;
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 *
	 * @return Menu instance.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey) {
		return menu(textKey, (String) null);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 *
	 * @return Menu instance, with optional graphic.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey, @Nullable String imagePath) {
		return menu(textKey, imagePath, false);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param antialias
	 * 		Flag to enable anti-aliasing of the image graphic.
	 *
	 * @return Menu instance, with optional graphic.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey, @Nullable String imagePath, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		Menu menu = new Menu();
		menu.textProperty().bind(Lang.getBinding(textKey));
		menu.setGraphic(graphic);
		menu.setId(textKey);
		return menu;
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param icon
	 * 		Ikonli icon for the menu graphic.
	 *
	 * @return Menu instance, with graphic.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey, @Nonnull Ikon icon) {
		return menu(textKey, icon, null);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param icon
	 * 		Ikonli icon for the menu graphic.
	 * @param color
	 * 		Color for icon.
	 *
	 * @return Menu instance, with graphic.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey, @Nonnull Ikon icon, @Nullable Color color) {
		FontIconView graphic = color == null ?
				new FontIconView(icon) :
				new FontIconView(icon, IconView.DEFAULT_ICON_SIZE, color);
		return menu(textKey, graphic);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param graphic
	 * 		Optional menu graphic.
	 *
	 * @return Menu instance, with optional graphic.
	 */
	@Nonnull
	public static Menu menu(@Nonnull String textKey, @Nullable Node graphic) {
		Menu menu = new Menu();
		menu.textProperty().bind(Lang.getBinding(textKey));
		menu.setGraphic(graphic);
		menu.setId(textKey);
		return menu;
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Menu instance, with behavior on-click.
	 */
	@Nonnull
	public static Menu actionMenu(@Nonnull String textKey, @Nullable String imagePath, @Nonnull Runnable runnable) {
		return actionMenu(textKey, imagePath, runnable, false);
	}

	/**
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
	@Nonnull
	public static Menu actionMenu(@Nonnull String textKey, @Nullable String imagePath,
								  @Nonnull Runnable runnable, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		return new ActionMenu(Lang.getBinding(textKey), graphic, runnable).withId(textKey);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nonnull Runnable runnable) {
		return action(textKey, (String) null, runnable);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nullable String imagePath, @Nonnull Runnable runnable) {
		return action(textKey, imagePath, runnable, false);
	}

	/**
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
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nullable String imagePath,
										@Nonnull Runnable runnable, boolean antialias) {
		Node graphic = imagePath == null ? null :
				antialias ? Icons.getScaledIconView(imagePath) : Icons.getIconView(imagePath);
		return action(textKey, graphic, runnable);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param icon
	 * 		Ikonli icon for the menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nonnull Ikon icon, @Nonnull Runnable runnable) {
		return action(textKey, icon, null, runnable);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param icon
	 * 		Ikonli icon for the menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nonnull Ikon icon, @Nullable Color color, @Nonnull Runnable runnable) {
		FontIconView graphic = color == null ?
				new FontIconView(icon) :
				new FontIconView(icon, IconView.DEFAULT_ICON_SIZE, color);
		return action(textKey, graphic, runnable);
	}

	/**
	 * @param textKey
	 * 		Translation key.
	 * @param graphic
	 * 		Menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull String textKey, @Nullable Node graphic, @Nonnull Runnable runnable) {
		return action(Lang.getBinding(textKey), graphic, runnable).withId(textKey);
	}

	/**
	 * @param textBinding
	 * 		Menu text binding.
	 * @param graphic
	 * 		Menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem action(@Nonnull StringBinding textBinding, @Nullable Node graphic, @Nonnull Runnable runnable) {
		return new ActionMenuItem(textBinding, graphic, runnable);
	}

	/**
	 * @param text
	 * 		Menu item text.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem actionLiteral(@Nullable String text, @Nullable String imagePath, @Nonnull Runnable runnable) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new ActionMenuItem(text, graphic, runnable);
	}

	/**
	 * @param text
	 * 		Menu item text.
	 * @param icon
	 * 		Ikonli icon for the menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	@Nonnull
	public static ActionMenuItem actionLiteral(@Nullable String text, @Nonnull Ikon icon, @Nonnull Runnable runnable) {
		FontIconView graphic = new FontIconView(icon);
		return new ActionMenuItem(text, graphic, runnable);
	}

	/**
	 * @return New menu separator.
	 */
	@Nonnull
	public static SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}