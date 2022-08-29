package me.coley.recaf.ui.control.config;

import com.google.common.primitives.Ints;
import javafx.beans.property.IntegerProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.bounds.IntLowerBound;
import me.coley.recaf.config.bounds.IntUpperBound;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * Spinner for setting an integer property.
 *
 * @author Amejonah
 */
public class ConfigInt extends Spinner<Integer> implements Unlabeled {
	/**
	 * @param container
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigInt(ConfigContainer container, Field field) {
		int min = getMin(field);
		int max = getMax(field);
		getStylesheets().add("/style/spinner.css");
		int value = 0;
		if (IntegerProperty.class.isAssignableFrom(field.getType())) {
			IntegerProperty property = ReflectUtil.quietGet(container, field);
			value = property.get();
			property.bind(valueProperty());
		} else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
			value = ReflectUtil.quietGet(container, field);
			valueProperty().addListener((observable, oldValue, newValue) -> ReflectUtil.quietSet(container, field, newValue));
		} else {
			throw new IllegalArgumentException("Field " + container.getClass().getName() + "#" + field.getName() + " must be of type int, Integer or IntegerProperty");
		}
		setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, Math.max(min, Math.min(value, max))));
		getEditor().setTextFormatter(new TextFormatter<>(change -> {
			Integer v = Ints.tryParse(change.getControlNewText());
			if (v == null || !(min <= v && v <= max)) {
				return null;
			}
			return change;
		}));
		setEditable(true);
	}

	private int getMin(Field field) {
		IntLowerBound lowerBound = field.getAnnotation(IntLowerBound.class);
		return lowerBound != null ? lowerBound.value() : Integer.MIN_VALUE;
	}

	private int getMax(Field field) {
		IntUpperBound upperBound = field.getAnnotation(IntUpperBound.class);
		return upperBound != null ? upperBound.value() : Integer.MAX_VALUE;
	}
}
