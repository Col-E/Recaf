package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import software.coley.observables.ObservableBoolean;

/**
 * Checkbox with binding to an {@link ObservableBoolean}.
 *
 * @author Matt Coley
 */
public class ObservableCheckBox extends CheckBox implements Tooltipable {
	/**
	 * @param observable
	 * 		Value to bind to.
	 * @param binding
	 * 		Text binding.
	 */
	public ObservableCheckBox(@Nonnull ObservableBoolean observable, @Nonnull ObservableValue<String> binding) {
		setSelected(observable.getValue());
		selectedProperty().addListener((ob, old, cur) -> observable.setValue(cur));
		textProperty().bind(binding);
	}
}
