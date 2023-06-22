package software.coley.recaf.ui.docking.listener;

import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingTab} being selected.
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
	void onSelection(DockingRegion region, DockingTab tab);
}
