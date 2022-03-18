package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockTab;

/**
 * Listener for {@link DockTab} being closed.
 *
 * @author Matt Coley
 */
public interface TabClosureListener {
	/**
	 * @param tab
	 * 		Tab closed.
	 */
	void onClose(DockTab tab);
}
