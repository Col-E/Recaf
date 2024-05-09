package software.coley.recaf.ui.docking;

import atlantafx.base.theme.Styles;
import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import com.panemu.tiwulfx.control.dock.TabDropHint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.*;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.Icons;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base docking tab-pane.
 *
 * @author Matt Coley
 */
public class DockingRegion extends DetachableTabPane {
	private static final Logger logger = Logging.get(DockingRegion.class);
	private static int counter = 0;
	private final int id = counter++;
	private DockingManager manager;

	DockingRegion(@Nonnull DockingManager manager) {
		this.manager = manager;
		setDropHint(new RecafDropHint());
		setStageFactory((priorParent, tab) -> {
			// Factory must yield a tab-stage, so we can't use 'RecafStage' here.
			// Setting an icon is all we really need though.
			TabStage stage = new TabStage(priorParent, tab);
			stage.getIcons().add(Icons.getImage(Icons.LOGO));
			return stage;
		});
		getStyleClass().add(Styles.DENSE);

		// It's easier to hook region creation like this than to try and ensure registration is handled by the caller.
		manager.registerRegion(this);
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
	@Nonnull
	public DockingTab createTab(@Nonnull String title, @Nonnull Node content) {
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
	@Nonnull
	public DockingTab createTab(@Nonnull ObservableValue<String> title, @Nonnull Node content) {
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
	@Nonnull
	public DockingTab createTab(@Nonnull Supplier<DockingTab> factory) {
		DockingTab tab = factory.get();

		// Ensure we record tabs closing (and let them still declare close handlers via the factory)
		EventHandler<Event> closeHandler = tab.getOnClosed();
		tab.setOnCloseRequest(e -> {
			// We use on-close-request so that 'getRegion' still has a reference to the containing tab-pane.
			// Using on-close, the region reference is removed.
			if (tab.isClosable()) {
				DockingRegion region = tab.getRegion();
				if (region != null && region.getDockTabs().contains(tab)) {
					if (closeHandler != null)
						closeHandler.handle(e);
					region.getAndCheckManager().onTabClose(region, tab);
				}
			}
		});

		// Ensure the manager is aware of which tabs per region are selected.
		DockingRegion thisRegion = this;
		EventHandler<Event> defaultSelectionHandler = tab.getOnSelectionChanged();
		registerSelectionCallbacks(tab, defaultSelectionHandler, thisRegion);

		// Ensure we record tabs moving between regions.
		tab.tabPaneProperty().addListener((observable, oldValue, newValue) -> {
			// Always record the last non-null parent region.
			if (oldValue instanceof DockingRegion oldParent)
				tab.setPriorRegion(oldParent);

			// Handle moving to a new region.
			//
			// Note: Do not mark the tab as 'closed' if the new parent is 'null'.
			// It will be null while the tab is being dragged.
			if (newValue instanceof DockingRegion newParent) {
				// Re-register the selection callbacks with the new parent.
				registerSelectionCallbacks(tab, defaultSelectionHandler, newParent);

				// Call tab-move listener if a past region has been recorded.
				DockingRegion priorRegion = tab.getPriorRegion();
				if (priorRegion != null)
					// Use the new parent to get the manager, as the old region could be closed.
					newParent.getAndCheckManager().onTabMove(priorRegion, newParent, tab);
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
		getAndCheckManager().onTabCreate(this, tab);
		return tab;
	}

	/**
	 * Sets the tab-selection listener to fire off {@link DockingManager#onTabSelection(DockingRegion, DockingTab)}.
	 * Must be called when a tab's parent changes to ensure the parent region it looks up <i>(in fallback cases)</i>
	 * is not out of date.
	 *
	 * @param tab
	 * 		Tab to set the selection change handler of.
	 * @param defaultSelectionHandler
	 * 		Default tab selection change handler.
	 * @param fallbackRegion
	 * 		Region context to use if the current tab-pane parent is not found/viable.
	 */
	private static void registerSelectionCallbacks(@Nonnull DockingTab tab, @Nullable EventHandler<Event> defaultSelectionHandler,
												   @Nonnull DockingRegion fallbackRegion) {
		tab.setOnSelectionChanged(e -> {
			if (defaultSelectionHandler != null)
				defaultSelectionHandler.handle(e);

			if (tab.isSelected()) {
				// Use the tab's parent, but if none is set then we are likely spawning the tab in 'this' region.
				TabPane parentPane = tab.getTabPane();
				if (parentPane instanceof DockingRegion parentRegion) {
					// Update with the tab's most up-to-date parent region isntance if possible
					parentRegion.getAndCheckManager().onTabSelection(parentRegion, tab);
				} else if (parentPane == null) {
					// If the parent is null, we're likely in the initialization of a given tab.
					// We'll assume it'll be added to 'this' region.
					fallbackRegion.getAndCheckManager().onTabSelection(fallbackRegion, tab);
				} else {
					logger.error("Could not update tab selection state for tab '{}'" +
							", parent pane was not docking region.", tab.getText());
				}
			}
		});
	}

	/**
	 * @return List of all docking tabs in the region.
	 */
	@Nonnull
	public List<DockingTab> getDockTabs() {
		// We do not want to use a 'stream.map()' operation just to satisfy generic contracts.
		// We know the tab implementations are all dock-tabs so this unsafe operation is OK.
		return Unchecked.cast(super.getTabs());
	}

	/**
	 * @return {@code true} when this region has been marked as closed.
	 */
	public boolean isClosed() {
		return manager == null;
	}

	/**
	 * Called when this region is closed.
	 */
	void onClose() {
		// We no longer need a reference to the manager that spawned this region.
		manager = null;
	}

	/**
	 * Exists so we don't directly reference the manager, which will NPE after the region is closed.
	 *
	 * @return Manager instance.
	 */
	@Nonnull
	private DockingManager getAndCheckManager() {
		if (manager == null)
			throw new IllegalStateException("Region has been closed, the manager reference has been cleared");
		return manager;
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

	@Override
	public String toString() {
		// Incremental ID's makes debugging docking problems way easier.
		return "DockingRegion[" + id + "]" +
				(manager == null ? "(DEAD)" : "") +
				" - Children[" + getTabs().size() + "]";
	}
}
