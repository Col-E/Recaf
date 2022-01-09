package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

/**
 * Initializes user interaction/behavior of problem indicators.
 *
 * @author Matt Coley
 * @see ProblemIndicatorApplier Factory builder for the indicators.
 */
public class ProblemIndicatorInitializer {
	private final ProblemTracking problemTracking;

	/**
	 * @param problemTracking
	 * 		Parent problem tracking instance.
	 */
	public ProblemIndicatorInitializer(ProblemTracking problemTracking) {
		this.problemTracking = problemTracking;
	}

	/**
	 * Add on-hover behavior to the given error indicator.
	 *
	 * @param line
	 * 		Document line number.
	 * @param indicator
	 * 		Indicator node of the error.
	 */
	public void setupErrorIndicatorBehavior(int line, Node indicator) {
		ProblemInfo info = problemTracking.getProblem(line);
		if (info != null) {
			Tooltip tooltip = new Tooltip(info.getMessage());
			Tooltip.install(indicator, tooltip);
		}
	}
}
