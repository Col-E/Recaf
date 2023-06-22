package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.util.Icons;

/**
 * Toggle button for a {@link BooleanProperty}.
 *
 * @author Amejonah
 * @author Matt Coley
 */
public class BoundToggleIcon extends Button implements Tooltipable {
	/**
	 * @param graphic
	 * 		Button graphic path for {@link Icons#getIconView(String)}.
	 * @param property
	 * 		Property to bind to.
	 */
	public BoundToggleIcon(@Nonnull String graphic, @Nonnull BooleanProperty property) {
		this(Icons.getIconView(graphic), property);
	}

	/**
	 * @param graphic
	 * 		Button graphic.
	 * @param property
	 * 		Property to bind to.
	 */
	public BoundToggleIcon(@Nonnull Node graphic, @Nonnull BooleanProperty property) {
		setGraphic(graphic);
		setOnAction(e -> property.set(!property.get()));
		opacityProperty().bind(
				Bindings.when(property)
						.then(1.0)
						.otherwise(0.4)
		);
	}

	/**
	 * @param graphic
	 * 		Button graphic path for {@link Icons#getIconView(String)}.
	 * @param observable
	 * 		Observable to bind to.
	 */
	public BoundToggleIcon(@Nonnull String graphic, @Nonnull ObservableBoolean observable) {
		this(Icons.getIconView(graphic), observable);
	}

	/**
	 * @param graphic
	 * 		Button graphic.
	 * @param observable
	 * 		Observable to bind to.
	 */
	public BoundToggleIcon(@Nonnull Node graphic, @Nonnull ObservableBoolean observable) {
		setGraphic(graphic);
		setOnAction(e -> observable.setValue(!observable.getValue()));
		observable.addChangeListener((ob, old, cur) -> setOpacity(cur ? 1.0 : 0.4));
	}
}
