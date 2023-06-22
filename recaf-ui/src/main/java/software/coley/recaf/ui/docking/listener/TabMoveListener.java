package software.coley.recaf.ui.docking.listener;

import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingTab} moving between {@link DockingRegion}.
 *
 * @author Matt Coley
 */
public interface TabMoveListener {
	/**
	 * @param oldParent
	 * 		Prior parent region.
	 * @param newParent
	 * 		New parent region.
	 * @param tab
	 * 		Tab moved.
	 */
	void onMove(DockingRegion oldParent, DockingRegion newParent, DockingTab tab);
}
