package me.coley.recaf.ui.pane;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;

/**
 * Pane that automates placing content into the docking system through tab-panes.
 *
 * @author Matt Coley
 */
public class DockingWrapperPane extends BorderPane {
	private final DockTab tab;

	private DockingWrapperPane(ObservableValue<String> title, Node content, int width, int height) {
		RecafDockingManager docking = docking();
		// Create new tab-pane using the root so it's all hooked together
		//  - Add the intended content to this new tab-pane.
		DockingRegion region = docking.createRegion();
		tab = docking.createTabIn(region, () -> new DockTab(title, content));
		region.setCloseIfEmpty(true);
		// Remove any actions from history when creating this tab-pane.
		// We don't want new tabs to spawn in this new tab-pane.
		docking.removeInteractionHistory(region);
		// Set the tab-pane as the child of this 'BorderPane' implementation.
		setCenter(region);
		if (width >= 0 && height >= 0)
			setPrefSize(width, height);
	}

	/**
	 * @return Initially created tab.
	 */
	public DockTab getTab() {
		return tab;
	}

	/**
	 * @return Parent docking region.
	 */
	public DockingRegion getParentDockingRegion() {
		return tab.getParent();
	}

	/**
	 * @return Builder for the wrapper pane.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private static RecafDockingManager docking() {
		return RecafDockingManager.getInstance();
	}

	/**
	 * Builder for the wrapper pane.
	 */
	public static class Builder {
		private String key;
		private ObservableValue<String> title;
		private int width = -1;
		private int height = -1;
		private Node content;

		/**
		 * @return Creates the wrapper pane.
		 */
		public DockingWrapperPane build() {
			if (key == null)
				key = title.getValue();
			return new DockingWrapperPane(title, content, width, height);
		}

		/**
		 * @param title
		 * 		Tab title.
		 *
		 * @return Builder.
		 */
		public Builder title(ObservableValue<String> title) {
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
