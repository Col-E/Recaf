package software.coley.recaf.ui.control;

import atlantafx.base.util.IntegerStringConverter;
import jakarta.annotation.Nonnull;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import software.coley.observables.Observable;
import software.coley.observables.ObservableInteger;

/**
 * Spinner with binding to a numeric {@link Observable}.
 *
 * @param <T>
 * 		Numeric type.
 *
 * @author Matt Coley
 */
public class ObservableSpinner<T> extends Spinner<T> implements Tooltipable {
	private ObservableSpinner(@Nonnull Observable<T> observable) {
		valueProperty().addListener((ob, old, cur) -> observable.setValue(cur));
	}

	/**
	 * @param observable
	 * 		Observable to wrap.
	 * @param min
	 * 		Min value.
	 * @param max
	 * 		Max value.
	 *
	 * @return Spinner instance.
	 */
	public static ObservableSpinner<Integer> intSpinner(@Nonnull ObservableInteger observable, int min, int max) {
		int initial = observable.getValue();
		ObservableSpinner<Integer> spinner = new ObservableSpinner<>(observable);
		spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
		spinner.setEditable(true);
		IntegerStringConverter.createFor(spinner);
		return spinner;
	}
}
