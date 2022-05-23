package me.coley.recaf.ui.control.code;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Problem indicator applier. Shows problems sourced from a {@link ProblemTracking}.
 *
 * @author Matt Coley
 */
public class ProblemIndicatorApplier implements IndicatorApplier {
	private final SyntaxArea editor;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public ProblemIndicatorApplier(SyntaxArea editor) {
		this.editor = editor;
	}

	@Override
	public boolean checkModified(int lineNo, Polygon poly) {
		// Populate or hide it.
		if (editor.getProblemTracking() == null)
			throw new IllegalStateException("Should not have generated problem indicator," +
					" no problem tracking is associated with the control!");
		ProblemInfo problemInfo = editor.getProblemTracking().getProblem(lineNo);
		if (problemInfo != null) {
			// Change color based on severity
			switch (problemInfo.getLevel()) {
				case ERROR:
					poly.setFill(Color.RED);
					break;
				case WARNING:
					poly.setFill(Color.YELLOW);
					break;
				case INFO:
				default:
					poly.setFill(Color.LIGHTBLUE);
					break;
			}
			// Populate behavior
			ProblemIndicatorInitializer indicatorInitializer = editor.getProblemTracking().getIndicatorInitializer();
			if (indicatorInitializer != null) {
				indicatorInitializer.setupErrorIndicatorBehavior(lineNo, poly);
			}
			return true;
		}
		// Nothing to do
		return false;
	}
}
