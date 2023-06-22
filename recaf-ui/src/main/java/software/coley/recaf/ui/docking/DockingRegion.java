package software.coley.recaf.ui.docking;

import atlantafx.base.theme.Styles;
import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import com.panemu.tiwulfx.control.dock.TabDropHint;
import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.shape.*;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base docking tab-pane.
 *
 * @author Matt Coley
 */
public class DockingRegion extends DetachableTabPane {
	private static final Logger logger = Logging.get(DockingRegion.class);
	private final DockingManager manager;

	DockingRegion(@Nonnull DockingManager manager) {
		this.manager = manager;
		setDropHint(new RecafDropHint());
		getStyleClass().add(Styles.DENSE);
	}

	/**
	 * Add a tab to the region.
	 *
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	public DockingTab createTab(String title, Node content) {
		return createTab(() -> new DockingTab(title, content));
	}

	/**
	 * Add a tab to the region.
	 *
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	public DockingTab createTab(ObservableValue<String> title, Node content) {
		return createTab(() -> new DockingTab(title, content));
	}

	/**
	 * Add a tab to the region. The tab is constructed by the given factory.
	 *
	 * @param factory
	 * 		Factory that will provide the tab.
	 *
	 * @return Created tab.
	 */
	public DockingTab createTab(Supplier<DockingTab> factory) {
		DockingTab tab = factory.get();

		// Ensure we record tabs closing (and let them still declare close handlers via the factory)
		EventHandler<Event> closeHandler = tab.getOnClosed();
		tab.setOnCloseRequest(e -> {
			// We use on-close-request so that 'getRegion' still has a reference to the containing tab-pane.
			// Using on-close, the region reference is removed.
			if (tab.isClosable()) {
				if (closeHandler != null)
					closeHandler.handle(e);
				manager.onTabClose(tab.getRegion(), tab);
			}
		});

		// Ensure the manager is aware of which tabs per region are selected.
		DockingRegion thisRegion = this;
		EventHandler<Event> selectionHandler = tab.getOnSelectionChanged();
		tab.setOnSelectionChanged(e -> {
			if (selectionHandler != null)
				selectionHandler.handle(e);
			if (tab.isSelected()) {
				// Use the tab's parent, but if none is set then we are likely spawning the tab in 'this' region.
				if (tab.getTabPane() instanceof DockingRegion parentRegion) {
					manager.onTabSelection(parentRegion, tab);
				} else {
					manager.onTabSelection(thisRegion, tab);
				}
			}
		});

		// Ensure we record tabs moving between regions.
		tab.tabPaneProperty().addListener((observable, oldValue, newValue) -> {
			// Do not mark the tab as 'closed' if the new parent is 'null'.
			// It will be null while the tab is being dragged.
			if (oldValue instanceof DockingRegion oldParent &&
					newValue instanceof DockingRegion newParent) {
				manager.onTabMove(oldParent, newParent, tab);
			}
		});

		// Add the tab.
		ObservableList<Tab> tabs = getTabs();
		if (!tabs.contains(tab))
			tabs.add(tab);
		else
			logger.error("Tab was already added to region, prior to 'createTab' call.\n" +
							"Please ensure tabs are only added via 'createTab' to ensure consistent behavior.",
					new IllegalStateException("Stack dump"));
		manager.onTabCreate(this, tab);
		return tab;
	}

	/**
	 * @return List of all docking tabs in the region.
	 */
	@SuppressWarnings("unchecked")
	public List<DockingTab> getDockTabs() {
		// We do not want to use a 'stream.map()' operation just to satisfy generic contracts.
		// We know the tab implementations are all dock-tabs so this unsafe operation is OK.
		return (List<DockingTab>) (Object) super.getTabs();
	}

	/**
	 * This is a slightly modified version of the default {@link TabDropHint} which fixes some size constants.
	 */
	private static class RecafDropHint extends TabDropHint {
		@Override
		protected void generateAdjacentPath(Path path, double startX, double startY, double width, double height) {
			// no-op
		}

		@Override
		protected void generateInsertionPath(Path path, double tabPos, double width, double height) {
			int padding = 5;
			int tabHeight = 45;
			tabPos = Math.max(0, tabPos);
			path.getElements().clear();
			MoveTo moveTo = new MoveTo();
			moveTo.setX(padding);
			moveTo.setY(tabHeight);
			path.getElements().add(moveTo);//start

			path.getElements().add(new HLineTo(width - padding));//path width
			path.getElements().add(new VLineTo(height - padding));//path height
			path.getElements().add(new HLineTo(padding));//path bottom left
			path.getElements().add(new VLineTo(tabHeight));//back to start

			if (tabPos > 20) {
				path.getElements().add(new MoveTo(tabPos, tabHeight + 5));
				path.getElements().add(new LineTo(Math.max(padding, tabPos - 10), tabHeight + 15));
				path.getElements().add(new HLineTo(tabPos + 10));
				path.getElements().add(new LineTo(tabPos, tabHeight + 5));
			} else {
				double tip = Math.max(tabPos, padding + 5);
				path.getElements().add(new MoveTo(tip, tabHeight + 5));
				path.getElements().add(new LineTo(tip + 10, tabHeight + 5));
				path.getElements().add(new LineTo(tip, tabHeight + 15));
				path.getElements().add(new VLineTo(tabHeight + 5));
			}
		}
	}
}
