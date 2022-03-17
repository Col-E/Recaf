package me.coley.recaf.ui.docking.listener;

import me.coley.recaf.ui.docking.DockTab;

/**
 * Listener for {@link DockTab} being created.
 *
 * @author Matt Coley
 */
public interface TabCreationListener {
	/**
	 * @param tab
	 * 		Created tab.
	 */
	void onCreate(DockTab tab);
}
