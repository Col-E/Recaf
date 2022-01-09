package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Decorator factory for building various indicators.
 *
 * @author Matt Coley
 */
public class IndicatorFactory implements IntFunction<Node> {
	private static final double SIZE = 10;
	private static final double[] SHAPE = new double[]{
			0, 0,
			SIZE, SIZE / 2,
			0, SIZE};
	private final List<IndicatorApplier> indicatorAppliers = new ArrayList<>();
	private final SyntaxArea editor;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public IndicatorFactory(SyntaxArea editor) {
		this.editor = editor;
	}

	/**
	 * @param applier
	 * 		Applier to add.
	 */
	public void addIndicatorApplier(IndicatorApplier applier) {
		indicatorAppliers.add(applier);
	}

	/**
	 * @param applier
	 * 		Applier to remove.
	 *
	 * @return {@code true} when item was removed.
	 */
	public boolean removeIndicatorApplier(IndicatorApplier applier) {
		return indicatorAppliers.remove(applier);
	}

	/**
	 * Remove all indicator appliers.
	 */
	public void clear() {
		indicatorAppliers.clear();
	}

	@Override
	public Node apply(int lineNo) {
		if (editor.isParagraphFolded(lineNo)) {
			return null;
		}
		// Lines are addressed as how they visually appear (based on 1 being the beginning)
		lineNo++;
		// Create the indicator shape
		Polygon poly = new Polygon(SHAPE);
		poly.getStyleClass().add("cursor-pointer");
		boolean applied = false;
		for (IndicatorApplier applier : indicatorAppliers) {
			// Only one applier can act on a polygon at a time
			if (applier.apply(lineNo, poly)) {
				applied = true;
				break;
			}
		}
		// Hide if nothing happened
		if (!applied) {
			poly.setVisible(false);
		}
		return poly;
	}
}
