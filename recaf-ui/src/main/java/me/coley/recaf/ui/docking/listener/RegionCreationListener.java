package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockingManager;
import me.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for new {@link DockingRegion} being created for {@link DockingManager}.
 *
 * @author Matt Coley
 */
public interface RegionCreationListener {
	/**
	 * @param region
	 * 		Newly created region.
	 */
	void onCreate(DockingRegion region);
}
