package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.shape.Polygon;

import java.util.function.IntFunction;

/**
 * The paragraph line polygons generated should match the size of those in {@link ProblemIndicatorFactory}.
 * The reason why this exists is so that the different code-areas all have the same <i>"padding"</i> caused
 * by the inclusion of these line graphics.
 *
 * @author Matt Coley
 */
public class DummyIndicatorFactory implements IntFunction<Node> {
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
	public DummyIndicatorFactory(SyntaxArea editor) {
		this.editor = editor;
	}

	@Override
	public Node apply(int lineNo) {
		if (editor.isParagraphFolded(lineNo)) {
			return null;
		}
		Polygon poly = new Polygon(SHAPE);
		poly.getStyleClass().add("cursor-pointer");
		poly.setVisible(false);
		return poly;
	}
}
