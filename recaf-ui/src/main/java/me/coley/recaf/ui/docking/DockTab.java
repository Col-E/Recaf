package me.coley.recaf.ui.docking;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.util.threading.FxThreadUtil;

/**
 * {@link Tab} extension to track additional information required for {@link DockingManager} operations.
 *
 * @author Matt Coley
 */
public class DockTab extends Tab {
	private DockingRegion parent;

	/**
	 * @param title
	 * 		Initial tab title.
	 * 		Non-observable and effectively the final title text of the tab.
	 * @param content
	 * 		Initial tab content.
	 */
	public DockTab(String title, Node content) {
		textProperty().setValue(title);
		setContent(content);
	}

	/**
	 * @param title
	 * 		Initial tab title.
	 * @param content
	 * 		Initial tab content.
	 */
	public DockTab(ObservableValue<String> title, Node content) {
		textProperty().bind(title);
		setContent(content);
	}

	/**
	 * @param region
	 * 		New docking region parent.
	 */
	public void setParent(DockingRegion region) {
		parent = region;
	}

	/**
	 * @return Parent docking region that contains the tab.
	 */
	public DockingRegion getParent() {
		return parent;
	}

	/**
	 * Close the current tab.
	 */
	public void close() {
		TabPane tabPane = getTabPane();
		Event.fireEvent(this, new Event(Tab.CLOSED_EVENT));
		if (tabPane != null)
			FxThreadUtil.run(() -> tabPane.getTabs().remove(this));
	}

	/**
	 * Select the current tab.
	 */
	public void select() {
		if (parent != null)
			parent.getSelectionModel().select(this);
		if (getContent() != null)
			getContent().requestFocus();
	}
}
