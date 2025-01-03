package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.reactfx.value.Var;
import software.coley.collections.Unchecked;
import software.coley.recaf.util.NodeEvents;
import software.coley.recaf.util.ReflectUtil;

/**
 * Wrapper for {@link VirtualizedScrollPane} to properly expose properties with JavaFX's property types instead of
 * {@link Var} which cannot be used in a number of scenarios.
 *
 * @param <V>
 * 		Node type.
 *
 * @author Matt Coley
 */
public class VirtualizedScrollPaneWrapper<V extends Region & Virtualized> extends VirtualizedScrollPane<V> {
	private static final double AUTO_SCROLL_MULTIPLIER = 0.1;
	private static final double AUTO_SCROLL_BUFFER_PX = 5;
	private final DoubleProperty xScrollProperty = new SimpleDoubleProperty(0);
	private final DoubleProperty yScrollProperty = new SimpleDoubleProperty(0);
	private final BooleanProperty canAutoScroll = new SimpleBooleanProperty(true);
	private final ScrollBar horizontalScrollbar;
	private final ScrollBar verticalScrollbar;
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

	/**
	 * @param content
	 * 		Virtualized content.
	 */
	public VirtualizedScrollPaneWrapper(V content) {
		super(content);

		horizontalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(this, VirtualizedScrollPane.class.getDeclaredField("hbar")));
		verticalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(this, VirtualizedScrollPane.class.getDeclaredField("vbar")));

		setup();
	}

	private void setup() {
		xScrollProperty.bind(estimatedScrollXProperty());
		yScrollProperty.bind(estimatedScrollYProperty());

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

		// Get current scroll values
		double value = verticalScrollbar.getValue();
		double min = verticalScrollbar.getMin();
		double max = verticalScrollbar.getMax();

		// Calculate scroll amount based on viewport size
		double viewportHeight = getHeight();
		double scrollAmount = (deltaY * AUTO_SCROLL_MULTIPLIER);
		if (Math.abs(scrollAmount) > 0.1) {
			// Calculate new scroll position
			double newValue = value + scrollAmount;
			newValue = Math.max(min, Math.min(max, newValue));

			// Update scroll position
			verticalScrollbar.setValue(newValue);
		}
	}

	/**
	 * @return {@code true} when this scroll pane is auto-scrolling.
	 */
	public boolean isAutoScrolling() {
		return isAutoScrolling;
	}

	/**
	 * @return Horizontal scrollbar.
	 */
	@Nonnull
	public ScrollBar getHorizontalScrollbar() {
		return horizontalScrollbar;
	}

	/**
	 * @return Vertical scrollbar.
	 */
	@Nonnull
	public ScrollBar getVerticalScrollbar() {
		return verticalScrollbar;
	}

	/**
	 * @return Horizontal scroll property.
	 */
	@Nonnull
	public DoubleProperty horizontalScrollProperty() {
		return xScrollProperty;
	}

	/**
	 * @return Vertical scroll property.
	 */
	@Nonnull
	public DoubleProperty verticalScrollProperty() {
		return yScrollProperty;
	}

	/**
	 * @return Can auto-scroll property.
	 */
	@Nonnull
	public BooleanProperty canAutoScrollProperty() {
		return canAutoScroll;
	}
}
