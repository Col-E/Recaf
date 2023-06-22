package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;

/**
 * Toggle checkbox for a {@link BooleanProperty}.
 *
 * @author Matt Coley
 */
public class BoundCheckBox extends CheckBox implements Tooltipable {
	/**
	 * @param text
	 * 		Checkbox text.
	 * @param property
	 * 		Property to bind to.
	 */
	public BoundCheckBox(@Nonnull ObservableValue<String> text, @Nonnull BooleanProperty property) {
		textProperty().bind(text);
		selectedProperty().bindBidirectional(property);
	}
}
