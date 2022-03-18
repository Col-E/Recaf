package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockTab} being selected.
 *
 * @author Matt Coley
 */
public interface TabSelectionListener {
	/**
	 * @param region
	 * 		Parent region the tab belongs to.
	 * @param tab
	 * 		Tab selected.
	 */
	void onSelection(DockingRegion region, DockTab tab);
}
