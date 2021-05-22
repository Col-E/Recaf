package me.coley.recaf.ui.control.code;

/**
 * Listener to receive updates when a {@link ProblemTracking} adds or removes a problem.
 *
 * @author Matt Coley
 */
public interface ProblemUpdateListener {
	/**
	 * @param line
	 * 		Document line number.
	 * @param info
	 * 		Problem associated with the line.
	 */
	void onProblemAdded(int line, ProblemInfo info);

	/**
	 * @param line
	 * 		Document line number.
	 * @param info
	 * 		Problem removed from the line.
	 */
	void onProblemRemoved(int line, ProblemInfo info);
}
