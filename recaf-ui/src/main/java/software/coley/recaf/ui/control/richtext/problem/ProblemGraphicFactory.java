package software.coley.recaf.ui.control.richtext.problem;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
	/**
	 * New graphic factory.
	 */
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
		canvas.setMouseTransparent(true);
		canvas.setTranslateY(3);
		canvas.setTranslateX(toStart + 1.7);

		pane.getChildren().add(canvas);

		drawWaves(canvas, errorWidth);
	}

	private void drawWaves(Canvas canvas, double errorWidth) {
		var gc = canvas.getGraphicsContext2D();

		// make a solid red line
		gc.setStroke(Color.RED);
		gc.setLineWidth(1);

		gc.beginPath();

		final double scalingFactor = .7;
		final double waveUp = 3 * scalingFactor;
		final double waveDown = 6 * scalingFactor;
		final double stop = errorWidth - scalingFactor;

		// draw sawtooth pattern
		for (double offset = 0; offset < stop; offset += waveDown) {
			gc.moveTo(offset, containerHeight - waveUp);
			if (offset + waveUp < stop)
				gc.lineTo(offset + waveUp, containerHeight - waveDown);
			if (offset + waveDown < stop)
				gc.lineTo(offset + waveDown, containerHeight - waveUp);
		}

		// draw one last wave up
		gc.lineTo(stop, containerHeight - waveUp);

		gc.stroke();
		gc.closePath();
	}
}