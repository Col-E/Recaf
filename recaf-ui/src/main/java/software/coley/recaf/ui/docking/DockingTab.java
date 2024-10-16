package software.coley.recaf.ui.docking;

import com.panemu.tiwulfx.control.dock.DetachableTab;
import jakarta.annotation.Nullable;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import software.coley.recaf.util.FxThreadUtil;

/**
 * {@link Tab} extension to track additional information required for {@link DockingManager} operations.
 *
 * @author Matt Coley
 */
public class DockingTab extends DetachableTab {
	private volatile boolean firedClose;
	/** Last known associated region */
	private DockingRegion priorRegion;

	/**
	 * @param title
	 * 		Initial tab title.
	 * 		Non-observable and effectively the final title text of the tab.
	 * @param content
	 * 		Initial tab content.
	 */
	DockingTab(String title, Node content) {
		textProperty().setValue(title);
		setContent(content);
	}

	/**
	 * @param title
	 * 		Initial tab title.
	 * @param content
	 * 		Initial tab content.
	 */
	public DockingTab(ObservableValue<String> title, Node content) {
		textProperty().bind(title);
		setContent(content);
	}

	/**
	 * @return Parent docking region that contains the tab.
	 */
	@Nullable
	public DockingRegion getRegion() {
		return (DockingRegion) getTabPane();
	}

	/**
	 * @return Prior region.
	 */
	@Nullable
	public DockingRegion getPriorRegion() {
		return priorRegion;
	}

	/**
	 * @param priorRegion
	 * 		Prior region.
	 */
	void setPriorRegion(@Nullable DockingRegion priorRegion) {
		this.priorRegion = priorRegion;
	}

	/**
	 * Close the current tab.
	 */
	public void close() {
		if (isClosable()) {
			// Fire the close event if we've not done so yet.
			// The event should only be fired once, even if 'close()' is called multiple times.
			// We double-lock this to ensure the event really is only ever called once if 'close()' is ran across threads.
			boolean alreadyRequested = false;
			if (!firedClose) {
				synchronized (this) {
					if (!firedClose) {
						firedClose = true;

						// It is important that this event is the same as the one that the containing DockingRegion
						// registers a listener for. We use close requests instead of direct closes.
						Event.fireEvent(this, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
					} else {
						alreadyRequested = true;
					}
				}
			} else {
				alreadyRequested = true;
			}

			// We already triggered a close. No need to handle anything else below twice.
			if (alreadyRequested)
				return;

			// Remove from containing tab-pane.
			TabPane tabPane = getTabPane();
			if (tabPane != null) {
				// In cases where this is handled after the scene has been closed
				// we can skip this process as the docking library we use does not
				// consider such cases and throws an exception.
				ObservableList<Tab> tabList = tabPane.getTabs();
				if (tabPane.getScene() != null) {
					tabList.remove(this);
					priorRegion = null;
				}
			}
		}
	}

	/**
	 * Select the current tab.
	 */
	public void select() {
		TabPane parent = getTabPane();
		if (parent != null)
			parent.getSelectionModel().select(this);

		Node content = getContent();
		if (content != null)
			content.requestFocus();
	}
}