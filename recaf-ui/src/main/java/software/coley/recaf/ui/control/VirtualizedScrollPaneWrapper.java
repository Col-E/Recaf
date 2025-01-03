package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.animation.AnimationTimer;
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
	private final SimpleDoubleProperty xScrollProperty = new SimpleDoubleProperty(0);
	private final SimpleDoubleProperty yScrollProperty = new SimpleDoubleProperty(0);
	private final ScrollBar horizontalScrollbar;
	private final ScrollBar verticalScrollbar;
	private Cursor preAutoScrollCursor;
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
			if (e.getButton() == MouseButton.MIDDLE) {
				preAutoScrollCursor = getContent().getCursor();
				autoScrollTimer.start();
				autoScrollStartY = e.getScreenY();
				autoScrollCurrentY = autoScrollStartY;
				getContent().setCursor(Cursor.V_RESIZE);
			}
		});
		NodeEvents.addMouseReleaseHandler(getContent(), e -> {
			if (e.getButton() == MouseButton.MIDDLE) {
				getContent().setCursor(preAutoScrollCursor);
				autoScrollTimer.stop();
			}
		});
		NodeEvents.addMouseDraggedHandler(getContent(), e -> {
			if (e.getButton() == MouseButton.MIDDLE)
				autoScrollCurrentY = e.getScreenY();
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
	public SimpleDoubleProperty horizontalScrollProperty() {
		return xScrollProperty;
	}

	/**
	 * @return Vertical scroll property.
	 */
	@Nonnull
	public SimpleDoubleProperty verticalScrollProperty() {
		return yScrollProperty;
	}
}
