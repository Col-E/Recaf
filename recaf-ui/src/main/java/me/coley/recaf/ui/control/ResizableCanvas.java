package me.coley.recaf.ui.control;

import javafx.scene.canvas.Canvas;

/**
 * Allows a canvas to be put into an auto-sizing container such as {@link javafx.scene.layout.BorderPane}
 * without any needed manual property binds.
 *
 * @author Matt Coley
 */
public class ResizableCanvas extends Canvas {
	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public double maxHeight(double width) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double maxWidth(double height) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double minWidth(double height) {
		return 1;
	}

	@Override
	public double minHeight(double width) {
		return 1;
	}

	@Override
	public void resize(double width, double height) {
		setWidth(width);
		setHeight(height);
	}
}