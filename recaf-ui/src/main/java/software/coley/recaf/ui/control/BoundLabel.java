package software.coley.recaf.ui.control;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
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
	public BoundLabel(ObservableValue<String> binding) {
		textProperty().bind(binding);
	}
}
