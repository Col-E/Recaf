package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.util.List;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import me.coley.recaf.util.Reflect;

/**
 * Reflection-based combo-box.
 * 
 * @author Matt
 *
 * @param <T>
 */
public class ReflectiveCombo<T> extends ComboBox<T> {
	public ReflectiveCombo(Object instance, Field field, List<T> values, List<String> optStrs) {
		getItems().addAll(values);
		getSelectionModel().select(Reflect.get(instance, field));
		getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> Reflect.set(instance, field, newValue));
		// translate values into values from string list
		if (optStrs != null) {
			setConverter(new StringConverter<T>() {
				@Override
				public String toString(T value) {
					return optStrs.get(values.indexOf(value));
				}

				@Override
				public T fromString(String text) {
					return values.get(optStrs.indexOf(text));
				}
			});
		}
	}
}
