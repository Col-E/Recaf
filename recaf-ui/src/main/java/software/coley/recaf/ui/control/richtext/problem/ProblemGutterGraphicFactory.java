package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;

/**
 * Graphic factory that adds overlays to line graphics indicating the problem status of the line.
 *
 * @author Matt Coley
 * @see ProblemTracking
 */
public class ProblemGutterGraphicFactory extends AbstractLineGraphicFactory  {
	private Editor editor;

	/**
	 * New graphic factory.
	 */
	public ProblemGutterGraphicFactory() {
		super(LineGraphicFactory.P_LINE_PROBLEMS);
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		ProblemTracking problemTracking = editor.getProblemTracking();

		// Always null if no bracket tracking is registered for the editor.
		if (problemTracking == null) return;

		// Add problem graphic overlay to lines with problems.
		int line = paragraph + 1;
		Problem problem = problemTracking.getFirstProblemOnLine(line);
		if (problem != null) {
			ProblemLevel level = problem.level();
			Color levelColor = switch (level) {
				case ERROR -> Color.RED;
				case WARN -> Color.YELLOW;
				default -> Color.TURQUOISE;
			};
			Node graphic = switch (level) {
				case ERROR -> new FontIconView(CarbonIcons.ERROR, levelColor);
				case WARN -> new FontIconView(CarbonIcons.WARNING_ALT, levelColor);
				default -> new FontIconView(CarbonIcons.INFORMATION, levelColor);
			};

			// Workaround: When launching Recaf with some specific window scale values
			// a rounding error causes the shape to infinitely expand.
			// If we map the double value to an int, it acts as a floor operation and prevents the issue.
			Rectangle shape = new Rectangle();
			shape.widthProperty().bind(container.widthProperty().map(Number::intValue));
			shape.heightProperty().bind(container.heightProperty().map(Number::intValue));
			shape.setCursor(Cursor.HAND);
			shape.setFill(levelColor);
			shape.setOpacity(0.33);

			Tooltip tooltip = new Tooltip(formatTooltipMessage(problem));
			tooltip.setGraphic(graphic);
			switch (level) {
				case ERROR -> tooltip.getStyleClass().add("error-text");
				case WARN -> tooltip.getStyleClass().add("warn-text");
			}
			Tooltip.install(shape, tooltip);
			container.addTopLayer(shape);
		}
	}

	@Nonnull
	private static String formatTooltipMessage(@Nonnull Problem problem) {
		StringBuilder sb = new StringBuilder();
		int line = problem.line();
		if (line > 0) {
			int column = problem.column();
			if (column >= 0) {
				sb.append("Column ").append(column);
			}
		} else {
			sb.append("Unknown line");
		}
		return sb.append('\n').append(problem.message()).toString();
	}
}
