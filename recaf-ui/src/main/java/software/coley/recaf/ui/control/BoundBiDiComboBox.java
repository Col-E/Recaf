package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Combo box with two-way binding to a {@link ObjectProperty}.
 *
 * @param <T>
 * 		Property value type.
 *
 * @author Matt Coley
 * @see BoundComboBox One-way bound combo-box.
 */
public class BoundBiDiComboBox<T> extends ComboBox<T> implements Tooltipable {
	/**
	 * @param value
	 * 		property.
	 * @param values
	 * 		Available options.
	 * @param converter
	 * 		Value to string conversion.
	 */
	public BoundBiDiComboBox(@Nonnull Property<T> value, @Nonnull List<T> values, @Nonnull StringConverter<T> converter) {
		// Populate combo-model and select the initial value.
		getItems().addAll(values);
		SingleSelectionModel<T> selectionModel = getSelectionModel();
		selectionModel.select(value.getValue());

		// Bind the property to the given selected item. We have this intermediate wrapper to mimic two-way binding
		// on the selected item property, which is declared as read-only.
		ObjectProperty<T> currentItemWrapper = new SimpleObjectProperty<>();
		selectionModel.selectedItemProperty().addListener((ob, old, cur) -> currentItemWrapper.setValue(cur));
		value.addListener((ob, old, cur) -> selectionModel.select(cur));

		// Allow horizontal expansion.
		setMaxWidth(Double.MAX_VALUE);

		// T to String mapping.
		setConverter(converter);
	}
}
