package me.coley.recaf.ui.control;

import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;

import java.io.ByteArrayInputStream;

import static me.coley.recaf.ui.util.Menus.action;

/**
 * A wrapper around {@link ImageView} which allows an image to be panned <i>(using the primary mouse button)</i>
 * and scaled <i>(using the scroll wheel)</i>.
 *
 * @author Matt Coley
 */
public class PannableImageView extends BorderPane {
	private final ImageView view;
	private double startDragX;
	private double startDragY;
	private ContextMenu menu;

	/**
	 * Create without an image initially specified.
	 */
	public PannableImageView() {
		this((Image) null);
	}

	/**
	 * @param content
	 * 		Raw image data to display.
	 */
	public PannableImageView(byte[] content) {
		this(new Image(new ByteArrayInputStream(content)));
	}

	/**
	 * @param image
	 * 		Image to display.
	 */
	public PannableImageView(Image image) {
		view = new ImageView(image);
		// view.setImageSmoothing(false); // Added in JFX 12, fixes blur on zoom-in
		setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				startDragX = view.getTranslateX() - e.getX();
				startDragY = view.getTranslateY() - e.getY();
				setCursor(Cursor.MOVE);
			}
		});
		setOnMouseReleased(e -> {
			setCursor(Cursor.HAND);
		});
		setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				view.setTranslateX(startDragX + e.getX());
				view.setTranslateY(startDragY + e.getY());
			}
		});
		setOnScroll(e -> {
			double zoomSpeed = 0.05;
			// in some environments the scroll events are prefixed by an event with a delta of 0 before the actual event
			// which causes the below logic to assume it is a zoom out.
			// Since the change is 0 we can just ignore it and return here.
			if(e.getDeltaY() == 0)
				return;
			double zoomModifier = e.getDeltaY() > 0 ?
					1.0 + zoomSpeed : 1.00 - zoomSpeed;
			view.setScaleX(view.getScaleX() * zoomModifier);
			view.setScaleY(view.getScaleY() * zoomModifier);
			e.consume();
		});
		setCenter(view);
		setOnContextMenuRequested(this::onMenuRequested);
	}

	/**
	 * @param content
	 * 		Raw image data to display.
	 */
	public void setImage(byte[] content) {
		setImage(new Image(new ByteArrayInputStream(content)));
	}

	/**
	 * @param image
	 * 		Image to display.
	 */
	public void setImage(Image image) {
		view.setImage(image);
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
		}
		// Create menu if needed
		if (menu == null) {
			menu = new ContextMenu();
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			menu.getItems().add(action("menu.image.resetscale", this::resetScale));
			menu.getItems().add(action("menu.image.center", this::resetPosition));
		}
		// Show at new position
		menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
		menu.requestFocus();
	}

	private void resetScale() {
		view.setScaleX(1);
		view.setScaleY(1);
	}

	private void resetPosition() {
		view.setTranslateX(0);
		view.setTranslateY(0);
	}
}
