package me.coley.recaf.ui.pane;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;

/**
 * Pane that automates placing content into the docking system through tab-panes.
 *
 * @author Matt Coley
 */
public class DockingWrapperPane extends BorderPane {
	private final Tab tab;

	private DockingWrapperPane(String key, String title, int width, int height, Node content) {
		tab = new DockingRootPane.KeyedTab(key, title, content);
		DockingRootPane docking = docking();
		// Create new tab-pane using the root so it's all hooked together
		//  - Add the intended content to this new tab-pane.
		DetachableTabPane tabPane = docking.createNewTabPane();
		tabPane.getTabs().add(tab);
		tabPane.setCloseIfEmpty(true);
		// Remove any actions from history when creating this tab-pane.
		// We don't want new tabs to spawn in this new tab-pane.
		docking.removeFromHistory(tabPane);
		// Set the tab-pane as the child of this 'BorderPane' implementation.
		setCenter(tabPane);
		if (width >= 0 && height >= 0)
			setPrefSize(width, height);
	}

	/**
	 * @return Initially created tab.
	 */
	public Tab getTab() {
		return tab;
	}

	/**
	 * @return Parent tab-pane.
	 */
	public TabPane getParentTabPane() {
		return tab.getTabPane();
	}

	/**
	 * @return Builder for the wrapper pane.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private static DockingRootPane docking() {
		return RecafUI.getWindows().getMainWindow().getDockingRootPane();
	}

	/**
	 * Builder for the wrapper pane.
	 */
	public static class Builder {
		private String key;
		private String title;
		private int width = -1;
		private int height = -1;
		private Node content;

		/**
		 * @return Creates the wrapper pane.
		 */
		public DockingWrapperPane build() {
			if (key == null)
				key = title;
			return new DockingWrapperPane(key, title, width, height, content);
		}

		/**
		 * @param key
		 * 		Docking tab lookup key.
		 *
		 * @return Builder.
		 */
		public Builder key(String key) {
			this.key = key;
			return this;
		}

		/**
		 * @param title
		 * 		Tab title.
		 *
		 * @return Builder.
		 */
		public Builder title(String title) {
			this.title = title;
			return this;
		}

		/**
		 * @param width
		 * 		Content width.
		 * @param height
		 * 		Content height.
		 *
		 * @return Builder.
		 */
		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		/**
		 * @param content
		 * 		Content to place in the tab.
		 *
		 * @return Builder.
		 */
		public Builder content(Node content) {
			this.content = content;
			return this;
		}
	}
}
