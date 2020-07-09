package me.coley.recaf.ui.controls.popup;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;
import me.coley.recaf.util.ThreadUtil;

/**
 * Draggable popup window.
 *
 * @author Matt
 */
public class DragPopup {
	private final Popup pop = new Popup();

	/**
	 * @param content
	 * 		Popup content.
	 * @param handle
	 * 		Draggable header control.
	 */
	public DragPopup(ScrollPane content, Control handle) {
		this((Node) content, handle);
		content.getStyleClass().add("scroll-antiblur-hack");
		content.getStyleClass().add("drag-popup-scroll");
		content.getStyleClass().add("drag-popup");
		content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		content.setFitToWidth(true);
	}

	/**
	 * @param content
	 * 		Popup content.
	 * @param handle
	 * 		Draggable header control.
	 */
	public DragPopup(Node content, Control handle) {
		double[] xOffset = {0};
		double[] yOffset = {0};
		handle.getStyleClass().add("drag-popup-header");
		handle.setOnMousePressed(event -> {
			xOffset[0] = event.getSceneX();
			yOffset[0] = event.getSceneY();
		});
		handle.setOnMouseDragged(event -> {
			pop.setX(event.getScreenX() - xOffset[0]);
			pop.setY(event.getScreenY() - yOffset[0]);
		});
		BorderPane wrapper = new BorderPane();
		wrapper.getStyleClass().add("drag-popup-wrapper");
		wrapper.setCenter(content);
		wrapper.setTop(handle);
		wrapper.addEventHandler(MouseEvent.MOUSE_EXITED, e -> pop.hide());
		handle.prefWidthProperty().bind(wrapper.widthProperty());
		pop.getContent().setAll(wrapper);
		pop.setAutoHide(true);
	}

	/**
	 * Display the popup at the given location.
	 *
	 * @param parent
	 * 		Control to spawn on top of.
	 * @param x
	 * 		Screen x.
	 * @param y
	 * 		Screen y.
	 */
	public void show(Node parent, double x, double y) {
		pop.show(parent, x, y);
	}

	/**
	 * Display the popup at the center of the given parent.
	 *
	 * @param parent
	 * 		Control to spawn on top of.
	 */
	public void show(Node parent) {
		ThreadUtil.runJfxDelayed(100, () -> {
			pop.setOnShown(e -> {
				Bounds bounds = parent.localToScreen(parent.getBoundsInParent());
				double parentWidth = bounds.getMaxX() - bounds.getMinX();
				double parentHeight = bounds.getMaxY() - bounds.getMinY();
				double x = bounds.getMinX() + (parentWidth / 2);
				double y = bounds.getMinY() + (parentHeight / 2);
				pop.setX(x - pop.getWidth() / 2);
				pop.setY(y - pop.getHeight() / 2);
			});
			// Show only if parent component still exists (by checking its ownership)
			if (parent.getScene() != null && parent.getScene().getWindow() != null)
				pop.show(parent, 0, 0);
		});
	}

	protected void close() {
		pop.hide();
	}
}
