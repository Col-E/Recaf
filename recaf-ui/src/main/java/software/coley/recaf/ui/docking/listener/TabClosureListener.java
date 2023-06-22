package software.coley.recaf.ui.docking.listener;

import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingTab} being closed.
 *
 * @author Matt Coley
 */
public interface TabClosureListener {
	/**
	 * @param parent
	 * 		Parent region the tab belonged to.
	 * @param tab
	 * 		Tab closed.
	 */
	void onClose(DockingRegion parent, DockingTab tab);
}
