package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import software.coley.observables.Observable;
import software.coley.observables.ObservableObject;

import java.util.List;

/**
 * Combo box with binding to a {@link ObservableObject}.
 *
 * @param <T>
 * 		Observable value type.
 *
 * @author Matt Coley
 */
public class ObservableComboBox<T> extends ComboBox<T> implements Tooltipable {
	/**
	 * @param value
	 * 		Observable.
	 * @param values
	 * 		Available options.
	 */
	public ObservableComboBox(@Nonnull Observable<T> value, @Nonnull List<T> values) {
		getItems().addAll(values);
		getSelectionModel().select(value.getValue());
		SingleSelectionModel<T> model = getSelectionModel();
		value.addChangeListener((ob, old, cur) -> {
			if (model.getSelectedItem() != cur)
				model.select(cur);
		});
		model.selectedItemProperty().addListener((ob, old, cur) -> {
			if (value.getValue() != cur)
				value.setValue(cur);
		});
		setMaxWidth(Double.MAX_VALUE);
	}
}
