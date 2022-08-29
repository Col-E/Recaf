package me.coley.recaf.ui.control.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableBooleanValue;
import javafx.scene.control.CheckBox;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * Component for {@code boolean} config values.
 *
 * @author Matt Coley
 */
public class ConfigBoolean extends CheckBox {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 * @param text
	 * 		Text to represent the field.
	 */
	public ConfigBoolean(ConfigContainer instance, Field field, ObservableValue<String> text) {
		textProperty().bind(text);
		if (ObservableBooleanValue.class.isAssignableFrom(field.getType())) {
			ObservableBooleanValue value = ReflectUtil.quietGet(instance, field);
			if (value instanceof BooleanProperty)
				selectedProperty().bindBidirectional((BooleanProperty) value);
			else
				selectedProperty().addListener((observable, old, current) -> ((WritableBooleanValue) value).set(current));
		} else {
			setSelected(ReflectUtil.quietGet(instance, field));
			selectedProperty().addListener((observable, old, current) -> ReflectUtil.quietSet(instance, field, current));
		}
	}
}
