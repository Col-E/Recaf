package me.coley.recaf.ui.docking;

import java.util.function.Predicate;

/**
 * Filter used to determine which {@link DockingRegion} are viable to spawn a {@link DockTab} in.
 *
 * @author Matt Coley
 * @see DockingManager#createTab(RegionFilter, RegionPreference, DockTabFactory)
 */
public interface RegionFilter extends Predicate<DockingRegion> {
}
