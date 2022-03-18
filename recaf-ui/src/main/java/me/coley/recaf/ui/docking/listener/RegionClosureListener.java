package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockingManager;
import me.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingRegion} being closed/removed from {@link DockingManager}.
 *
 * @author Matt Coley
 */
public interface RegionClosureListener {
	/**
	 * @param region
	 * 		Region closed.
	 */
	void onClose(DockingRegion region);
}
