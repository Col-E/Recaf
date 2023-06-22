package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;

/**
 * Text field that forwards changes of the {@link #textProperty()} to a given {@link StringProperty}.
 *
 * @author Matt Coley
 */
public class BoundTextField extends TextField implements Tooltipable {
	/**
	 * @param destination
	 * 		Property to sync with text field contents.
	 */
	public BoundTextField(@Nonnull StringProperty destination) {
		// Initial state
		textProperty().set(destination.get());

		// Sync changes to destination
		destination.bind(textProperty());
	}
}
