package me.coley.recaf.ui.docking;

/**
 * Unchecked exception for when a {@link DockTab} cannot find a {@link DockingRegion} to spawn in when
 * {@link DockingManager#createTab(RegionFilter, RegionPreference, DockTabFactory)} is called.
 *
 * @author Matt Coley
 */
public class TabTargetException extends IllegalStateException {
	/**
	 * @param message
	 * 		Exception message.
	 */
	public TabTargetException(String message) {
		super(message);
	}
}
