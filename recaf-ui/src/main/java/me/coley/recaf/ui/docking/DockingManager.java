package me.coley.recaf.ui.docking;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import me.coley.recaf.ui.docking.listener.*;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Management utility wrapping {@link DetachableTabPane}.
 * Allows for application-wide tracking of {@link DockingRegion} and {@link DockTab}.
 *
 * @author Matt Coley
 */
public class DockingManager {
	private static final boolean DEBUG = false;
	private static final Logger logger = Logging.get(DockingManager.class);
	private final List<DockingRegion> dockingRegions = new ArrayList<>();
	private final List<DockingRegion> recentInteractions = new ArrayList<>();
	private final Map<Integer, DockTab> regionSelectedTabs = new HashMap<>();
	private final List<RegionCreationListener> regionCreationListeners = new ArrayList<>();
	private final List<RegionClosureListener> regionClosureListeners = new ArrayList<>();
	private final List<RegionUpdateListener> regionUpdateListeners = new ArrayList<>();
	private final List<TabSelectionListener> tabSelectionListeners = new ArrayList<>();
	private final List<TabCreationListener> tabCreationListeners = new ArrayList<>();
	private final List<TabClosureListener> tabClosureListeners = new ArrayList<>();
	private final List<TabMoveListener> tabMoveListeners = new ArrayList<>();
	private RegionFactory regionFactory = new RegionFactory(this);

	/**
	 * @return New created region.
	 */
	public DockingRegion createRegion() {
		return getRegionFactory().createAndInit();
	}

	/**
	 * @param region
	 * 		Region to place the tab in. Must be an instance from {@link #createRegion()}.
	 * @param factory
	 * 		Tab factory to supply the tab.
	 *
	 * @return Tab spawned in the given region.
	 */
	public DockTab createTabIn(DockingRegion region, DockTabFactory factory) {
		return createTab(r -> r == region, factory);
	}

	/**
	 * @param factory
	 * 		Tab factory to supply the tab.
	 *
	 * @return Tab which is spawned in the most recently interacted with region.
	 */
	public DockTab createTab(DockTabFactory factory) {
		return createTab(r -> true, factory);
	}

	/**
	 * @param filter
	 * 		Filter to narrow down which regions are viable to spawn in.
	 * @param factory
	 * 		Tab factory to supply the tab.
	 *
	 * @return Tab which is spawned in the most recently interacted with region that matches the filter.
	 */
	public DockTab createTab(RegionFilter filter, DockTabFactory factory) {
		Optional<DockingRegion> regionResult = recentInteractions.stream().filter(filter).findFirst();
		if (regionResult.isEmpty())
			throw new IllegalStateException("Cannot spawn tab, no viable region to spawn in!");
		DockingRegion region = regionResult.get();
		return region.createTab(decorateFactory(region, factory));
	}

	/**
	 * Called by {@link #createTab(RegionFilter, DockTabFactory)}.
	 * Used by child types of {@link DockingManager} to modify properties of any created tabs.
	 *
	 * @param region
	 * 		Region the tab will belong to.
	 * @param factory
	 * 		Factory to decorate.
	 *
	 * @return The factory instance if there is no special handling.
	 * Otherwise, a wrapper factory that adds additional properties to its generated tabs.
	 */
	protected DockTabFactory decorateFactory(DockingRegion region, DockTabFactory factory) {
		return factory;
	}

	/**
	 * @return All docking regions.
	 */
	public List<DockingRegion> getDockingRegions() {
		return dockingRegions;
	}

	/**
	 * @return All docking tabs open in all docking regions.
	 */
	public List<DockTab> getAllTabs() {
		return dockingRegions.stream()
				.flatMap(region -> region.getDockTabs().stream())
				.collect(Collectors.toList());
	}

	/**
	 * @return Recent interacted with docking regions. Earlier indices more are recent.
	 */
	public List<DockingRegion> getRecentInteractions() {
		return recentInteractions;
	}

	/**
	 * @return The selected tabs of each region.
	 */
	public Map<Integer, DockTab> getRegionSelectedTabs() {
		return regionSelectedTabs;
	}

	/**
	 * @return Factory used to create {@link DockingRegion} instances.
	 */
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	/**
	 * @param regionFactory
	 * 		Factory used to create {@link DockingRegion} instances.
	 */
	public void setRegionFactory(RegionFactory regionFactory) {
		this.regionFactory = regionFactory;
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addRegionCreationListener(RegionCreationListener listener) {
		regionCreationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeRegionCreationListener(RegionCreationListener listener) {
		return regionCreationListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addRegionClosureListener(RegionClosureListener listener) {
		regionClosureListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeRegionClosureListener(RegionClosureListener listener) {
		return regionClosureListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addRegionUpdateListener(RegionUpdateListener listener) {
		regionUpdateListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeRegionUpdateListener(RegionUpdateListener listener) {
		return regionUpdateListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabSelectionListener(TabSelectionListener listener) {
		tabSelectionListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabSelectionListener(TabSelectionListener listener) {
		return tabSelectionListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabCreationListener(TabCreationListener listener) {
		tabCreationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabCreationListener(TabCreationListener listener) {
		return tabCreationListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabClosureListener(TabClosureListener listener) {
		tabClosureListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabClosureListener(TabClosureListener listener) {
		return tabClosureListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addTabMoveListener(TabMoveListener listener) {
		tabMoveListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} upon removal. {@code false} when listener wasn't present.
	 */
	public boolean removeTabMoveListener(TabMoveListener listener) {
		return tabMoveListeners.remove(listener);
	}

	/**
	 * @param region
	 * 		Region to remove from {@link #getRecentInteractions()}.
	 */
	public void removeInteractionHistory(DockingRegion region) {
		popInteraction(region);
	}

	void pushInteraction(DockingRegion region) {
		recentInteractions.remove(region);
		recentInteractions.add(0, region);
	}

	void popInteraction(DockingRegion region) {
		recentInteractions.remove(region);
	}

	void onTabCreate(DockTab tab, DockingRegion parent) {
		tab.setParent(parent);
		for (TabCreationListener listener : tabCreationListeners)
			listener.onCreate(tab);
		for (RegionUpdateListener listener : regionUpdateListeners)
			listener.onTabAdded(parent, tab);
		pushInteraction(parent);
		if (DEBUG) logger.trace("Tab created: {}", tab.getText());
	}

	void onTabClose(DockTab tab) {
		DockingRegion oldRegion = tab.getParent();
		tab.setParent(null);
		for (TabClosureListener listener : tabClosureListeners)
			listener.onClose(tab);
		for (RegionUpdateListener listener : regionUpdateListeners)
			listener.onTabRemoved(oldRegion, tab);
		if (DEBUG) logger.trace("Tab closed: {}", tab.getText());
	}

	void onTabMove(DockTab tab, DockingRegion oldRegion, DockingRegion newRegion) {
		tab.setParent(newRegion);
		for (TabMoveListener listener : tabMoveListeners)
			listener.onMove(tab, oldRegion, newRegion);
		for (RegionUpdateListener listener : regionUpdateListeners) {
			listener.onTabRemoved(oldRegion, tab);
			listener.onTabAdded(newRegion, tab);
		}
		pushInteraction(newRegion);
		if (DEBUG) logger.trace("Tab moved: '{}' from '{}' -> '{}'",
				tab.getText(), oldRegion.getRegionId(), newRegion.getRegionId());
	}

	void onTabSelection(DockingRegion region, DockTab tab) {
		int id = region.getRegionId();
		regionSelectedTabs.put(id, tab);
		for (TabSelectionListener listener : tabSelectionListeners)
			listener.onSelection(region, tab);
		if (DEBUG) logger.trace("Region selected tab: '{}' = '{}'", id, tab.getText());
	}

	void onRegionCreate(DockingRegion region) {
		dockingRegions.add(region);
		for (RegionCreationListener listener : regionCreationListeners)
			listener.onCreate(region);
		pushInteraction(region);
		if (DEBUG) logger.trace("Region created: {}", region.getRegionId());
	}

	void onRegionClose(DockingRegion region) {
		dockingRegions.remove(region);
		for (RegionClosureListener listener : regionClosureListeners)
			listener.onClose(region);
		popInteraction(region);
		if (DEBUG) logger.trace("Region closed: {}", region.getRegionId());
		// Needed in case a window containing the region gets closed
		for (DockTab tab : region.getDockTabs())
			onTabClose(tab);
	}
}
