package me.coley.recaf.ui.docking;

import java.util.Comparator;

/**
 * Used to sort multiple potential valid regions matched by a {@link RegionFilter}.
 *
 * @author Matt Coley
 * @see DockingManager#createTab(RegionFilter, RegionPreference, DockTabFactory)
 */
public interface RegionPreference extends Comparator<DockingRegion> {
}
