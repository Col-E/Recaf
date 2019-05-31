package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import javafx.scene.control.TextField;
import me.coley.recaf.util.Reflect;

/**
 * Reflection-based textfield. Input that can be converted into the target type
 * is set via reflection.
 * 
 * @author Matt
 *
 * @param <T>
 */
public abstract class ReflectiveTextField<T> extends TextField {
	public ReflectiveTextField(Object instance, Field field) {
		setText(instance, field);
		textProperty().addListener((ob, old, curr) -> {
			T converted = convert(curr);
			if (converted != null) {
				set(instance, field, converted);
			}
		});
	}

	/**
	 * Set text-field text from instance and field.
	 * 
	 * @param instance
	 * @param field
	 */
	protected void setText(Object instance, Field field) {
		T value = Reflect.get(instance, field);
		this.setText(convert(value));
	}

	/**
	 * Set value.
	 * 
	 * @param instance
	 *            Instance where value should be applied to field.
	 * @param field
	 *            Field to set.
	 * @param converted
	 *            Value to set.
	 */
	protected void set(Object instance, Field field, T converted) {
		Reflect.set(instance, field, converted);
	}

	/**
	 * @param text
	 * @return Value from text.
	 */
	protected abstract T convert(String text);

	/**
	 * @param value
	 * @return Text from value.
	 */
	protected abstract String convert(T value);
}
