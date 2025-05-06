package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Utility label to quickly apply a {@link StringBinding}, commonly for UI translations.
 *
 * @author Matt Coley
 */
public class BoundLabel extends Label implements Tooltipable {
	/**
	 * @param binding
	 * 		Text binding.
	 */
	public BoundLabel(@Nonnull ObservableValue<String> binding) {
		textProperty().bind(binding);
	}

	/**
	 * @param binding
	 * 		Text binding.
	 * @param graphic
	 * 		Label display icon.
	 */
	public BoundLabel(@Nonnull ObservableValue<String> binding, @Nonnull Node graphic) {
		textProperty().bind(binding);
		setGraphic(graphic);
	}

	/**
	 * Unbinds the old text property and rebinds to the given value.
	 *
	 * @param binding
	 * 		New value to bind to.
	 */
	public void rebind(@Nonnull ObservableValue<String> binding) {
		var property = textProperty();
		property.unbind();
		property.bind(binding);
	}
}
