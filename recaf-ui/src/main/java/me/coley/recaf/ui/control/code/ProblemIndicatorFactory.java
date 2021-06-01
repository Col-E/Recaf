package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.function.IntFunction;

/**
 * Decorator factory for building problem indicators.
 *
 * @author Matt Coley
 */
public class ProblemIndicatorFactory implements IntFunction<Node> {
	private static final double SIZE = 10;
	private static final double[] SHAPE = new double[]{
		0, 0,
		SIZE, SIZE / 2,
		0, SIZE};
	private final SyntaxArea editor;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public ProblemIndicatorFactory(SyntaxArea editor) {
		this.editor = editor;
	}

	@Override
	public Node apply(int lineNo) {
		if (editor.isParagraphFolded(lineNo)) {
			return null;
		}
		// Lines are addressed as how they visually appear (based on 1 being the beginning)
		lineNo++;
		// Create the problem/error shape
		Polygon poly = new Polygon(SHAPE);
		poly.getStyleClass().add("cursor-pointer");
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
		} else {
			poly.setVisible(false);
		}
		return poly;
	}
}
