package software.coley.recaf.ui.control.richtext.inheritance;

import software.coley.recaf.behavior.PrioritySortable;

/**
 * Listener to receive updates when any change occurs in a {@link InheritanceTracking}'s tracked inheritances map.
 *
 * @author Matt Coley
 */
public interface InheritanceInvalidationListener extends PrioritySortable {
	/**
	 * Called when any changes to tracked inheritances occurs.
	 */
	void onInheritanceInvalidation();
}