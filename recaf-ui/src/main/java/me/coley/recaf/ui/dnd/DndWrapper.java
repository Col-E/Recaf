package me.coley.recaf.ui.dnd;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.scene.Node;
import javafx.scene.control.Tab;

/**
 * Tab wrapper for drag-and-drop controls.
 */
public class DndWrapper extends DetachableTabPane {
	private DndWrapper() {
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
	public static DndWrapper closable(String title, Node node) {
		DndWrapper wrapper = new DndWrapper();
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
	public static DndWrapper locked(String title, Node node) {
		DndWrapper wrapper = closable(title, node);
		wrapper.getTabs().get(0).setClosable(false);
		return wrapper;
	}
}
