package me.coley.recaf.ui.util;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Used to add mouse listeners to a {@link Region} and make it resizable by the user by
 * clicking and dragging the border in the same way as a window.
 *
 * @author Andy Till - Original implementation
 * @author Matt Coley - Modifications
 */
public class DragResizer {
	/**
	 * The margin around the control that a user can click in to start resizing the region.
	 */
	private static final int RESIZE_MARGIN = 25;
	private final Region region;
	private double x;
	private double y;
	private boolean initHeight;
	private boolean initWidth;
	private boolean dragging;

	private DragResizer(Region region) {
		this.region = region;
	}

	/**
	 * @param region
	 * 		Region to make resizable.
	 */
	public static void makeResizable(Region region) {
		makeResizable(region, -1, -1);
	}

	/**
	 * @param region
	 * 		Region to make resizable.
	 * @param minWidth
	 * 		Minimum size.
	 * @param minHeight
	 * 		Minimum size.
	 */
	public static void makeResizable(Region region, double minWidth, double minHeight) {
		DragResizer resizer = new DragResizer(region);
		region.setOnMousePressed(resizer::mousePressed);
		region.setOnMouseDragged(resizer::mouseDragged);
		region.setOnMouseReleased(resizer::mouseReleased);
		region.setOnMouseMoved(resizer::mouseOver);
		// Using 'entered' to intercept when mouse comes in from the side where the scrollbar is.
		// The scrollbar consumes the mouse-move but not this 'enter' call.
		// This allows the resizing zone to occupy the same space as the bottom of the scrollbar.
		region.setOnMouseEntered(resizer::mouseOver);
		// Initialize min sizes if given
		if (minWidth > RESIZE_MARGIN) {
			region.setMinWidth(minWidth);
		}
		if (minHeight > RESIZE_MARGIN) {
			region.setMinHeight(minHeight);
		}
	}

	protected void mouseReleased(MouseEvent event) {
		dragging = false;
		region.setCursor(Cursor.DEFAULT);
	}

	protected void mouseOver(MouseEvent event) {
		boolean withinZone = isInDraggableZone(event);
		if (withinZone || dragging) {
			region.setCursor(Cursor.SE_RESIZE);
		} else {
			region.setCursor(Cursor.DEFAULT);
		}
		// Prevent child components from intercepting mouse events while the mouse
		// is within the draggable zone.
		for (Node child : region.getChildrenUnmodifiable()) {
			child.setMouseTransparent(withinZone);
		}
	}

	protected boolean isInDraggableZone(MouseEvent event) {
		return event.getX() > (region.getWidth() - RESIZE_MARGIN) &&
				event.getY() > (region.getHeight() - RESIZE_MARGIN);
	}

	protected void mouseDragged(MouseEvent event) {
		if (!dragging)
			return;
		double mX = event.getX();
		double mY = event.getY();
		double newWidth = Math.max(region.getMinWidth(), region.getPrefWidth() + (mX - x));
		double newHeight = Math.max(region.getMinHeight(), region.getPrefHeight() + (mY - y));
		region.setPrefWidth(newWidth);
		region.setPrefHeight(newHeight);
		x = mX;
		y = mY;
	}

	protected void mousePressed(MouseEvent event) {
		// Ignore clicks outside the draggable margin
		if (!isInDraggableZone(event))
			return;
		// Mark dragging
		dragging = true;
		// Make sure that the minimum height is set to the current height once,
		// setting a min height that is smaller than the current height will
		// have no effect.
		if (!initWidth) {
			region.setPrefWidth(region.getWidth());
			initWidth = true;
		}
		if (!initHeight) {
			region.setPrefHeight(region.getHeight());
			initHeight = true;
		}
		x = event.getX();
		y = event.getY();
	}
}