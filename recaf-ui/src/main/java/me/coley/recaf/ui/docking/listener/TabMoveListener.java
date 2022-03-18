package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockTab} moving between {@link DockingRegion}.
 *
 * @author Matt Coley
 */
public interface TabMoveListener {
	/**
	 * @param tab
	 * 		Tab moved.
	 * @param oldParent
	 * 		Prior parent region.
	 * @param newParent
	 * 		New parent region.
	 */
	void onMove(DockTab tab, DockingRegion oldParent, DockingRegion newParent);
}
