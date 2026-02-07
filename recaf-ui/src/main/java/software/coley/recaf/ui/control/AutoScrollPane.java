package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import software.coley.recaf.util.NodeEvents;

/**
 * Scroll pane extension that adds support for middle-click auto-scrolling.
 *
 * @author Matt Coley
 * @see VirtualizedScrollPaneWrapper
 */
public class AutoScrollPane extends ScrollPane {
	private static final double AUTO_SCROLL_MULTIPLIER = 0.1;
	private static final double AUTO_SCROLL_BUFFER_PX = 5;
	private final BooleanProperty canAutoScroll = new SimpleBooleanProperty(true);
	private Cursor preAutoScrollCursor;
	private boolean isAutoScrolling;
	private double autoScrollStartY;
	private double autoScrollCurrentY;
	private final AnimationTimer autoScrollTimer = new AnimationTimer() {
		@Override
		public void handle(long now) {
			updateAutoScroll();
		}
	};

	public AutoScrollPane(@Nonnull Node content) {
		super(content);

		// Handle middle mouse press to start auto-scrolling.
		// - Press initiates the auto-scroll
		// - Drag changes the auto-scroll speed
		// - Release stops the auto-scroll
		NodeEvents.addMousePressHandler(getContent(), e -> {
			if (canAutoScroll.get() && e.getButton() == MouseButton.MIDDLE) {
				preAutoScrollCursor = getContent().getCursor();
				autoScrollStartY = e.getScreenY();
				autoScrollCurrentY = autoScrollStartY;
			}
		});
		NodeEvents.addMouseReleaseHandler(getContent(), e -> {
			if (e.getButton() == MouseButton.MIDDLE && isAutoScrolling()) {
				getContent().setCursor(preAutoScrollCursor);
				autoScrollTimer.stop();
				autoScrollCurrentY = -1;
				isAutoScrolling = false;
			}
		});
		NodeEvents.addMouseDraggedHandler(getContent(), e -> {
			if (e.getButton() == MouseButton.MIDDLE) {
				autoScrollCurrentY = e.getScreenY();

				// Only begin the auto-scroll after the user has moved a couple of pixels away.
				//
				// We do this because the 'content' node may have middle click behavior similar
				// to how a browser opens/closes tabs when using the middle mouse button on links.
				// By not initiating until we're sure the user intends to move around via auto-scroll
				// we don't mess with the UX of the existing behavior in the 'content' node.
				if (!isAutoScrolling && Math.abs(autoScrollCurrentY - autoScrollStartY) > AUTO_SCROLL_BUFFER_PX) {
					isAutoScrolling = true;
					autoScrollTimer.start();
					getContent().setCursor(Cursor.V_RESIZE);
				}
			}
		});
	}

	private void updateAutoScroll() {
		double deltaY = autoScrollCurrentY - autoScrollStartY;
		double viewportHeight = getHeight();

		// Get current scroll values
		double contentSize = getContent() instanceof Region r ? r.getHeight() : viewportHeight;
		double value = getVvalue();
		double min = getVmin();
		double max = getVmax();

		// Calculate scroll amount based on viewport size
		double scrollAmount = (deltaY * AUTO_SCROLL_MULTIPLIER) / contentSize;
		if (Math.abs(scrollAmount) > 0) {
			// Calculate new scroll position
			double newValue = value + scrollAmount;
			newValue = Math.max(min, Math.min(max, newValue));

			// Update scroll position
			setVvalue(newValue);
		}
	}

	/**
	 * @return {@code true} when this scroll pane is auto-scrolling.
	 */
	public boolean isAutoScrolling() {
		return isAutoScrolling;
	}
}
