package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * Wraps a {@link Node} providing panning and zoom capabilities.
 *
 * @author Matt Coley
 */
public class PannableView extends Pane {
	private static final double MIN_ZOOM = 0.001;
	private static final double MAX_ZOOM = 100;
	private final BorderPane nodeWrapper;
	private double startDragX;
	private double startDragY;
	private double initTranslateX;
	private double initTranslateY;
	private double translateX;
	private double translateY;

	/**
	 * @param node
	 * 		Node to wrap.
	 */
	public PannableView(@Nonnull Node node) {
		nodeWrapper = new BorderPane(node);
		getChildren().addAll(nodeWrapper);
		setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				startDragX = translateX - e.getX();
				startDragY = translateY - e.getY();
				setCursor(Cursor.MOVE);
			}
		});
		setOnMouseReleased(e -> {
			setCursor(Cursor.HAND);
		});
		setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				translateX = startDragX + e.getX();
				translateY = startDragY + e.getY();

				// Translations get applied to our wrapper.
				// Managing the relationship between translation and zoom on one level is too much of a hassle.
				nodeWrapper.setTranslateX(translateX);
				nodeWrapper.setTranslateY(translateY);
			}
		});
		setOnScroll(e -> {
			// Handle zoom on the actual node itself, rather than on our wrapper
			zoom(node, e);
			e.consume();
		});
	}

	/**
	 * Sets the initial values to reset back to when calling {@link #resetTranslation()}.
	 *
	 * @param x
	 * 		Initial translation X.
	 * @param y
	 * 		Initial translation Y.
	 */
	public void setInitTranslation(double x, double y) {
		initTranslateX = x;
		initTranslateY = y;
		resetTranslation();
	}

	/**
	 * Reset translation offset to 0.
	 */
	public void resetTranslation() {
		translateX = initTranslateX;
		translateY = initTranslateY;

	    nodeWrapper.setTranslateX(initTranslateX);
	    nodeWrapper.setTranslateY(initTranslateY);

		Node node = nodeWrapper.getCenter();
		node.setTranslateX(0);
		node.setTranslateY(0);
	}

	/**
	 * Reset zoom level to 0.
	 */
	public void resetZoom() {
		Node node = nodeWrapper.getCenter();
		node.setScaleX(1);
		node.setScaleY(1);
		node.setTranslateX(0);
		node.setTranslateY(0);
	}

	/**
	 * @param node
	 * 		Node to zoom into.
	 * @param event
	 * 		Scroll event to extract zoom info from.
	 */
	private static void zoom(@Nonnull Node node, @Nonnull ScrollEvent event) {
		zoom(node, Math.pow(1.01, event.getDeltaY()), event.getSceneX(), event.getSceneY());
	}

	/**
	 * See: <a href="https://stackoverflow.com/questions/27356577/scale-at-pivot-point-in-an-already-scaled-node">
	 * Jens-Peter Haack's zoom</a>
	 *
	 * @param node
	 * 		Node to zoom into.
	 * @param factor
	 * 		Factor of zoom to apply.
	 * @param x
	 * 		Position to zoom into.
	 * @param y
	 * 		Position to zoom into.
	 */
	private static void zoom(@Nonnull Node node, double factor, double x, double y) {
		double oldScale = node.getScaleX();
		double scale = oldScale * factor;

		if (scale < MIN_ZOOM) scale = MIN_ZOOM;
		if (scale > MAX_ZOOM) scale = MAX_ZOOM;
		node.setScaleX(scale);
		node.setScaleY(scale);

		double f = (scale / oldScale) - 1;
		Bounds bounds = node.localToScene(node.getBoundsInLocal());
		double dx = (x - (bounds.getWidth() / 2 + bounds.getMinX()));
		double dy = (y - (bounds.getHeight() / 2 + bounds.getMinY()));

		node.setTranslateX(node.getTranslateX() - f * dx);
		node.setTranslateY(node.getTranslateY() - f * dy);
	}
}
