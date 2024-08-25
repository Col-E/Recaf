package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.docking.listener.TabClosureListener;
import software.coley.recaf.ui.docking.listener.TabCreationListener;
import software.coley.recaf.ui.docking.listener.TabMoveListener;
import software.coley.recaf.ui.docking.listener.TabSelectionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Manages open docking regions and tabs.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DockingManager {
	private static final Logger logger = Logging.get(DockingManager.class);
	private final DockingRegionFactory factory = new DockingRegionFactory(this);
	private final DockingRegion primaryRegion;
	private final List<DockingRegion> regions = new CopyOnWriteArrayList<>();
	private final List<TabSelectionListener> tabSelectionListeners = new CopyOnWriteArrayList<>();
	private final List<TabCreationListener> tabCreationListeners = new CopyOnWriteArrayList<>();
	private final List<TabClosureListener> tabClosureListeners = new CopyOnWriteArrayList<>();
	private final List<TabMoveListener> tabMoveListeners = new CopyOnWriteArrayList<>();

	@Inject
	public DockingManager() {
		primaryRegion = newRegion();
		primaryRegion.setCloseIfEmpty(false);
	}

	/**
	 * The primary region is where tabs should open by default.
	 * It is locked to the main window where the initial workspace info is displayed.
	 * Compared to an IDE, this would occupy the same space where open classes are shown.
	 *
	 * @return Primary region.
	 */
	@Nonnull
	public DockingRegion getPrimaryRegion() {
		return primaryRegion;
	}

	/**
	 * @return All current docking regions.
	 */
	@Nonnull
	public List<DockingRegion> getRegions() {
		return regions;
	}

	/**
	 * @return All current docking tabs.
	 */
	@Nonnull
	public List<DockingTab> getDockTabs() {
		return regions.stream().flatMap(r -> r.getDockTabs().stream()).toList();
	}

	/**
	 * @return New region.
	 */
	@Nonnull
	public DockingRegion newRegion() {
		// The docking region constructor should handle registration in the regions list.
		DockingRegion region = factory.create();
		factory.init(region);
		return region;
	}

	/**
	 * Package-private so {@link DockingRegion#DockingRegion(DockingManager)} has access.
	 *
	 * @param region
	 * 		Region to register.
	 */
	void registerRegion(@Nonnull DockingRegion region) {
		if (!regions.contains(region))
			regions.add(region);
	}

	/**
	 * Configured by {@link DockingRegionFactory} this method is called when a {@link DockingRegion} is closed.
	 *
	 * @param region
	 * 		Region being closed.
	 *
	 * @return {@code true} when region closure was a success.
	 * {@code false} when a region denied closure.
	 */
	boolean onRegionClose(@Nonnull DockingRegion region) {
		// Close any tabs that are closable.
		// If there are tabs that cannot be closed, deny region closure.
		boolean allowClosure = true;
		for (DockingTab tab : new ArrayList<>(region.getDockTabs()))
			if (tab.isClosable())
				tab.close();
			else
				allowClosure = false;
		if (!allowClosure)
			return false;

		// Update internal state.
		regions.remove(region);

		// Needed in case a window containing the region gets closed.
		for (DockingTab tab : new ArrayList<>(region.getDockTabs()))
			tab.close();

		// Tell the region it is closed, removing its reference to this docking manager.
		region.onClose();

		// Closure allowed.
		return true;
	}

	/**
	 * Configured by {@link DockingRegion#createTab(Supplier)}, called when a tab is created.
	 *
	 * @param parent
	 * 		Parent region.
	 * @param tab
	 * 		Tab created.
	 */
	void onTabCreate(@Nonnull DockingRegion parent, @Nonnull DockingTab tab) {
		Unchecked.checkedForEach(tabCreationListeners, listener -> listener.onCreate(parent, tab),
				(listener, t) -> logger.error("Exception thrown when opening tab '{}'", tab.getText(), t));
	}

	/**
	 * Configured by {@link DockingRegion#createTab(Supplier)}, called when a tab is closed.
	 *
	 * @param parent
	 * 		Parent region.
	 * @param tab
	 * 		Tab created.
	 */
	void onTabClose(@Nonnull DockingRegion parent, @Nonnull DockingTab tab) {
		// TODO: In some cases the listeners need to be called on the FX thread
		Unchecked.checkedForEach(tabClosureListeners, listener -> listener.onClose(parent, tab),
				(listener, t) -> logger.error("Exception thrown when closing tab '{}'", tab.getText(), t));
	}

	/**
	 * Configured by {@link DockingRegion#createTab(Supplier)}, called when a tab is
	 * moved between {@link DockingRegion}s.
	 *
	 * @param oldRegion
	 * 		Prior parent region.
	 * @param newRegion
	 * 		New parent region.
	 * @param tab
	 * 		Tab created.
	 */
	void onTabMove(@Nonnull DockingRegion oldRegion, @Nonnull DockingRegion newRegion, @Nonnull DockingTab tab) {
		Unchecked.checkedForEach(tabMoveListeners, listener -> listener.onMove(oldRegion, newRegion, tab),
				(listener, t) -> logger.error("Exception thrown when moving tab '{}'", tab.getText(), t));
	}

	/**
	 * Configured by {@link DockingRegion#createTab(Supplier)}, called when a tab is selected.
	 *
	 * @param parent
	 * 		Parent region.
	 * @param tab
	 * 		Tab created.
	 */
	void onTabSelection(@Nonnull DockingRegion parent, @Nonnull DockingTab tab) {
		Unchecked.checkedForEach(tabSelectionListeners, listener -> listener.onSelection(parent, tab),
				(listener, t) -> logger.error("Exception thrown when selecting tab '{}'", tab.getText(), t));
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabSelectionListener(@Nonnull TabSelectionListener listener) {
		tabSelectionListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabSelectionListener(@Nonnull TabSelectionListener listener) {
		return tabSelectionListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabCreationListener(@Nonnull TabCreationListener listener) {
		tabCreationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabCreationListener(@Nonnull TabCreationListener listener) {
		return tabCreationListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabClosureListener(@Nonnull TabClosureListener listener) {
		tabClosureListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabClosureListener(@Nonnull TabClosureListener listener) {
		return tabClosureListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabMoveListener(@Nonnull TabMoveListener listener) {
		tabMoveListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabMoveListener(@Nonnull TabMoveListener listener) {
		return tabMoveListeners.remove(listener);
	}
}
