package me.coley.recaf.ui.control;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.scene.Node;
import javafx.scene.control.Tab;

/**
 * Tab wrapper for drag-and-drop controls.
 *
 * @author Matt Coley
 */
public class DockableTab extends DetachableTabPane {
	private DockableTab() {
	}

	/**
	 * Create a tab of a closable node.
	 *
	 * @param title
	 * 		Tab title.
	 * @param node
	 * 		Tab content.
	 *
	 * @return Closeable DnD tab with given title/content.
	 */
	public static DockableTab closable(String title, Node node) {
		DockableTab wrapper = new DockableTab();
		wrapper.getTabs().add(new Tab(title, node));
		return wrapper;
	}

	/**
	 * Create a tab of a non-closable node.
	 *
	 * @param title
	 * 		Tab title.
	 * @param node
	 * 		Tab content.
	 *
	 * @return Non-closable DnD tab with given title/content.
	 */
	public static DockableTab locked(String title, Node node) {
		DockableTab wrapper = closable(title, node);
		wrapper.getTabs().get(0).setClosable(false);
		return wrapper;
	}
}
