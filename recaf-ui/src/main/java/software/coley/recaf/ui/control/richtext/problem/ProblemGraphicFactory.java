package software.coley.recaf.ui.control.richtext.problem;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractTextBoundLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;


/**
 * Graphic factory that adds overlays to line graphics indicating the problem status of the line.
 *
 * @author Matt Coley
 * @see ProblemTracking
 */
public class ProblemGraphicFactory extends AbstractTextBoundLineGraphicFactory {

	public ProblemGraphicFactory() {
		super(LineGraphicFactory.P_LINE_PROBLEMS);
	}

	@Override
	protected void apply(StackPane pane, int paragraph) {
		if (editor.getProblemTracking() == null)
			return;

		Problem problem = editor.getProblemTracking().getProblem(paragraph + 1);

		if (problem == null)
			return;

		// compute the width of the draw canvas and offset required
		int index = problem.column() + 1;
		double toStart = editor.computeWidthUntilCharacter(paragraph, index);
		double toEnd = editor.computeWidthUntilCharacter(paragraph, index + problem.length());
		toEnd = Math.max(toEnd, toStart + 6);
		double errorWidth = toEnd - toStart;

		// Create the overlay
		Canvas canvas = new Canvas(toEnd, containerHeight);
		canvas.setManaged(false);
		canvas.setTranslateY(3);
		canvas.setTranslateX(toStart + 1.7);

		pane.getChildren().add(canvas);

		drawWaves(canvas, problem.level() == ProblemLevel.WARN, errorWidth);

		Tooltip tooltip = new Tooltip(problem.message());
		Tooltip.install(canvas, tooltip);
	}

	private void drawWaves(Canvas canvas, boolean warn, double errorWidth) {
		var gc = canvas.getGraphicsContext2D();

		// make a solid red line
		gc.setStroke(warn ? Color.YELLOW : Color.RED);
		gc.setLineWidth(1);

		gc.beginPath();

		final double scalingFactor = .7;
		// heights
		final double waveUp = containerHeight - 3 * scalingFactor;
		final double waveDown = containerHeight - 6 * scalingFactor;

		final double stepScale = .6;
		final double step = waveDown * stepScale;
		final double halfStep = step / 2;

		final double downStop = errorWidth - halfStep;
		final double upStop = errorWidth - step;

        // we want to draw waves such that the last wave is on the border of the errorWidth
		for (double x = 0; x < errorWidth; x += step) {
			gc.moveTo(x, waveUp);
			if (x < downStop)
				gc.lineTo(x + halfStep, waveDown);
			if (x < upStop)
				gc.lineTo(x + step, waveUp);
		}

		gc.stroke();
		gc.closePath();
	}
}