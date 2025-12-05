package software.coley.recaf.ui.control.richtext.problem;

import software.coley.recaf.behavior.PrioritySortable;

/**
 * Listener to receive updates when any change occurs in a {@link ProblemTracking}'s tracked problem map.
 *
 * @author Matt Coley
 */
public interface ProblemInvalidationListener extends PrioritySortable {
	/**
	 * Called when any changes to tracked problems occurs.
	 */
	void onProblemInvalidation();
}
