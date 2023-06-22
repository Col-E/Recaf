package software.coley.recaf.ui.docking.listener;

import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.docking.DockingRegion;

/**
 * Listener for {@link DockingTab} being created.
 *
 * @author Matt Coley
 */
public interface TabCreationListener {
	/**
	 * @param parent
	 * 		Parent docking region.
	 * @param tab
	 * 		Created tab.
	 */
	void onCreate(DockingRegion parent, DockingTab tab);
}
