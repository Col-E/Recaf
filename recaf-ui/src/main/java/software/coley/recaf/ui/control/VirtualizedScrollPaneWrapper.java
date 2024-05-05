package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Region;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.reactfx.value.Var;

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
	private final SimpleDoubleProperty xScroll = new SimpleDoubleProperty(0);
	private final SimpleDoubleProperty yScroll = new SimpleDoubleProperty(0);

	/**
	 * @param content
	 * 		Virtualized content.
	 */
	public VirtualizedScrollPaneWrapper(V content) {
		super(content);
		setup();
	}

	private void setup() {
		xScroll.bind(estimatedScrollXProperty());
		yScroll.bind(estimatedScrollYProperty());
	}

	/**
	 * @return Horizontal scroll property.
	 */
	@Nonnull
	public SimpleDoubleProperty horizontalScrollProperty() {
		return xScroll;
	}

	/**
	 * @return Vertical scroll property.
	 */
	@Nonnull
	public SimpleDoubleProperty verticalScrollProperty() {
		return yScroll;
	}
}
