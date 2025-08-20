package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.IntegerProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Integer spinner with binding to a {@link IntegerProperty}.
 *
 * @author Matt Coley
 */
public class BoundIntSpinner extends Spinner<Integer> implements Tooltipable {
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance();

	/**
	 * @param value
	 * 		property.
	 */
	public BoundIntSpinner(@Nonnull IntegerProperty value) {
		final int initialValue = value.get();
		final String initialValueStr = Integer.toString(initialValue);

		setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE, initialValue));
		setEditable(true);
		setMaxWidth(Double.MAX_VALUE);
		getEditor().setText(initialValueStr);
		getEditor().setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), initialValue, BoundIntSpinner::handleChange));
		value.bind(valueProperty());
	}

	@Nullable
	private static TextFormatter.Change handleChange(@Nonnull TextFormatter.Change c) {
		if (c.isContentChange()) {
			ParsePosition pos = new ParsePosition(0);
			// NumberFormat evaluates the beginning of the text.
			BoundIntSpinner.NUMBER_FORMAT.parse(c.getControlNewText(), pos);
			if (pos.getIndex() == 0 || pos.getIndex() < c.getControlNewText().length()) {
				// Reject parsing the complete text failed.
				return null;
			}
		}
		return c;
	}
}
