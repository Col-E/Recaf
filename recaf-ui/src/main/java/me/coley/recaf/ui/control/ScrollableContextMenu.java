package me.coley.recaf.ui.control;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.layout.Region;

/**
 * Context menu that can be limited to a size using:
 * <ul>
 *     <li>{@link #setMaxHeight(double)}</li>
 * </ul>
 *
 * @author <a href="https://stackoverflow.com/a/58542568>Finn von Holten</a>
 */
public class ScrollableContextMenu extends ContextMenu {
	/**
	 * New scrollable menu.
	 */
	public ScrollableContextMenu() {
		addEventHandler(Menu.ON_SHOWING, e -> {
			Node content = getSkin().getNode();
			if (content instanceof Region) {
				((Region) content).setMaxHeight(getMaxHeight());
			}
		});
	}
}
