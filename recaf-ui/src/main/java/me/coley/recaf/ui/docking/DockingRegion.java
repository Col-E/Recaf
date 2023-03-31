package me.coley.recaf.ui.docking;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.event.Event;
import javafx.event.EventHandler;

import java.util.List;

/**
 * {@link DetachableTabPane} extension to track additional information required for {@link DockingManager} operations.
 *
 * @author Matt Coley
 */
public class DockingRegion extends DetachableTabPane {
	private static int globalRegionCount;
	private final DockingManager manager;
	private final int regionId = globalRegionCount++;

	/**
	 * @param manager
	 * 		Parent manager to the region belongs to.
	 */
	public DockingRegion(DockingManager manager) {
		this.manager = manager;
	}

	/**
	 * Add a tab to the region. The tab is constructed by the given factory.
	 *
	 * @param factory
	 * 		Factory that will provide the tab.
	 *
	 * @return Created tab.
	 */
	public DockTab createTab(DockTabFactory factory) {
		DockTab tab = factory.get();

		// Ensure we record tabs closing (and let them still declare close handlers via the factory)
		EventHandler<Event> closeHandler = tab.getOnClosed();
		tab.setOnClosed(e -> {
			if (closeHandler != null)
				closeHandler.handle(e);
			manager.onTabClose(tab);
		});

		// Ensure the manager is aware of which tabs per region are selected.
		EventHandler<Event> selectionHandler = tab.getOnSelectionChanged();
		tab.setOnSelectionChanged(e -> {
			if (selectionHandler != null)
				selectionHandler.handle(e);
			if (tab.isSelected()) {
				// Use the tab's parent, but if none is set then we are likely spawning the tab in 'this' region.
				DockingRegion parent = tab.getParent();
				if (parent == null)
					parent = this;
				manager.onTabSelection(parent, tab);
			}
		});

		// Ensure we record tabs moving between regions.
		tab.tabPaneProperty().addListener((observable, oldValue, newValue) -> {
			// Do not mark the tab as 'closed' if the new parent is 'null'.
			// It will be null while the tab is being dragged.
			if (tab.getParent() != null && newValue instanceof DockingRegion) {
				DockingRegion newParent = (DockingRegion) newValue;
				manager.onTabMove(tab, tab.getParent(), newParent);
			}
		});
		getTabs().add(tab);
		manager.onTabCreate(tab, this);
		return tab;
	}

	/**
	 * @return List of all docking tabs in the region.
	 */
	@SuppressWarnings("unchecked")
	public List<DockTab> getDockTabs() {
		// We do not want to use a 'stream.map()' operation just to satisfy generic contracts.
		// We know the tab implementations are all dock-tabs so this unsafe operation is OK.
		return (List<DockTab>) (Object) super.getTabs();
	}

	/**
	 * @return Associated docking manager.
	 */
	public DockingManager getManager() {
		return manager;
	}

	/**
	 * @return Identifier of the region instance. Based on a global incrementing counter.
	 */
	public int getRegionId() {
		return regionId;
	}

	@Override
	public String toString() {
		return "DockingRegion{" +
				"regionId=" + regionId +
				", tabs=" + getDockTabs().size() + "}";
	}
}
