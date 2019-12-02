package me.coley.recaf.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;

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
		content.getStyleClass().add("scroll-antiblur-hack");
		content.getStyleClass().add("doc-scroll");
		content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		content.setFitToWidth(true);
		double[] xOffset = {0};
		double[] yOffset = {0};
		handle.getStyleClass().add("doc-header");
		handle.setOnMousePressed(event -> {
			xOffset[0] = event.getSceneX();
			yOffset[0] = event.getSceneY();
		});
		handle.setOnMouseDragged(event -> {
			pop.setX(event.getScreenX() - xOffset[0]);
			pop.setY(event.getScreenY() - yOffset[0]);
		});
		BorderPane wrapper = new BorderPane();
		wrapper.getStyleClass().add("doc-wrapper");
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
}
