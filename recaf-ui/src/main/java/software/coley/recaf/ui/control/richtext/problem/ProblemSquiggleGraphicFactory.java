package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import software.coley.bentofx.control.PixelCanvas;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractTextBoundLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;
import software.coley.recaf.util.Colors;

import java.util.List;

/**
 * Graphic factory that adds overlays squiggles to the editor text where problems occur.
 *
 * @author Matt Coley
 * @author Justus Garbe
 * @see ProblemTracking
 */
public class ProblemSquiggleGraphicFactory extends AbstractTextBoundLineGraphicFactory {
	public ProblemSquiggleGraphicFactory() {
		super(LineGraphicFactory.P_LINE_PROBLEM_SQUIGGLES);
	}

	@Override
	protected void apply(StackPane pane, int paragraph) {
		if (editor.getProblemTracking() == null)
			return;

		List<Problem> problems = editor.getProblemTracking().getProblemsOnLine(paragraph + 1);
		if (problems.isEmpty())
			return;

		for (Problem problem : problems)
			prepareProblem(problem, pane, paragraph);
	}

	/**
	 * Adds the given problem to the stack pane.
	 *
	 * @param problem
	 * 		Problem to add.
	 * @param pane
	 * 		Pane to add the problem display to.
	 * @param paragraph
	 * 		Current paragraph index.
	 */
	private void prepareProblem(@Nonnull Problem problem, @Nonnull StackPane pane, int paragraph) {
		// Compute the width of the draw canvas and offset required
		int index = problem.column() + 1;
		double toStart = editor.computeWidthUntilCharacter(paragraph, index);
		double toEnd = editor.computeWidthUntilCharacter(paragraph, index + problem.length());
		toEnd = Math.max(toEnd, toStart + 6);
		int errorWidth = (int) (toEnd - toStart);

		// Create the overlay
		PixelCanvas canvas = new PixelCanvas(errorWidth, containerHeight);
		canvas.setManaged(false);
		canvas.setMouseTransparent(true);
		canvas.resize(errorWidth, containerHeight);
		canvas.setTranslateX(toStart);
		canvas.setTranslateY(2);

		pane.getChildren().add(canvas);

		drawWaves(canvas, problem.level() == ProblemLevel.WARN, errorWidth);

		Tooltip tooltip = new Tooltip(problem.message());
		Tooltip.install(canvas, tooltip);
	}

	/**
	 * Draws a saw-tooth wave pattern for the given error level of the given width.
	 *
	 * @param canvas
	 * 		Drawing destination.
	 * @param warn
	 * 		Error level, either {@code true} for warning, or {@code false} for errors.
	 * @param width
	 * 		Length of waves to draw.
	 */
	private void drawWaves(@Nonnull PixelCanvas canvas, boolean warn, double width) {
		// Make a solid yellow/red line based on error level
		int color = Colors.argb(warn ? Color.YELLOW : Color.RED);

		/* // TODO: Reimplement squiggle drawing with the more simple PixelCanvas
		// wave heights
		final double scalingFactor = .7;
		final double waveUp = containerHeight - 3 * scalingFactor;
		final double waveDown = containerHeight - 6 * scalingFactor;

		final double stepScale = .6 - (scalingFactor / 5);
		final double step = waveDown * stepScale;
		final double halfStep = step / 2;

		final double downStop = width - halfStep;
		final double upStop = width - step;

		// We want to draw waves such that the last wave is on the border of the errorWidth
		for (double x = 0; x < width; x += step) {
			gc.moveTo(x, waveUp);
			if (x < downStop)
				gc.lineTo(x + halfStep, waveDown);
			if (x < upStop)
				gc.lineTo(x + step, waveUp);
		}*/

		final int lineWidth = 1;
		canvas.drawHorizontalLine(0, containerHeight - lineWidth, width, lineWidth, color);
		canvas.commit();
	}
}