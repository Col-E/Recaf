package me.coley.recaf.ui.control.code;

import javafx.scene.shape.Polygon;

/**
 * Plug in for {@link IndicatorFactory}.
 *
 * @author Matt Coley
 */
public interface IndicatorApplier {
	/**
	 * @param lineNo
	 * 		Line the polygon resides on.
	 * @param poly
	 * 		Polygon of indicator.
	 *
	 * @return {@code true} when the polygon has been modified and should be drawn.
	 */
	boolean checkModified(int lineNo, Polygon poly);
}
