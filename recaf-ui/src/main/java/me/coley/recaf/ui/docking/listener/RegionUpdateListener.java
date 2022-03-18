package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingRegion} tab state being updated.
 *
 * @author Matt Coley
 */
public interface RegionUpdateListener {
	/**
	 * @param region
	 * 		Region updated.
	 * @param tab
	 * 		Tab added.
	 */
	void onTabAdded(DockingRegion region, DockTab tab);

	/**
	 * @param region
	 * 		Region updated.
	 * @param tab
	 * 		Tab removed.
	 */
	void onTabRemoved(DockingRegion region, DockTab tab);
}
