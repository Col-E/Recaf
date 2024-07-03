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
public class ProblemGraphicFactory extends AbstractLineGraphicFactory {
	private Editor editor;

	/**
	 * New graphic factory.
	 */
	public ProblemGraphicFactory() {
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
		Problem problem = problemTracking.getProblem(line);
		if (problem != null) {
			ProblemLevel level = problem.getLevel();
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

			Rectangle shape = new Rectangle();
			shape.widthProperty().bind(container.widthProperty());
			shape.heightProperty().bind(container.heightProperty());
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

	private static String formatTooltipMessage(Problem problem) {
		StringBuilder sb = new StringBuilder();
		int line = problem.getLine();
		if (line > 0) {
			int column = problem.getColumn();
			if (column >= 0) {
				sb.append("Column ").append(column);
			}
		} else {
			sb.append("Unknown line");
		}
		return sb.append('\n').append(problem.getMessage()).toString();
	}
}