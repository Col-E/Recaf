package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Combo box with binding to a {@link ObjectProperty}.
 *
 * @param <T>
 * 		Property value type.
 *
 * @author Matt Coley
 */
public class BoundComboBox<T> extends ComboBox<T> implements Tooltipable {
	/**
	 * @param value
	 * 		property.
	 * @param values
	 * 		Available options.
	 * @param converter
	 * 		Value to string conversion.
	 */
	public BoundComboBox(@Nonnull ObjectProperty<T> value, @Nonnull List<T> values, @Nonnull StringConverter<T> converter) {
		getItems().addAll(values);
		SingleSelectionModel<T> selectionModel = getSelectionModel();
		selectionModel.select(value.getValue());
		value.bind(selectionModel.selectedItemProperty());
		setMaxWidth(Double.MAX_VALUE);
		setConverter(converter);
	}
}
