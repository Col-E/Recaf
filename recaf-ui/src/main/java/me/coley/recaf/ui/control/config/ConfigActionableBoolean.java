package me.coley.recaf.ui.control.config;

import javafx.beans.value.ObservableValue;
import me.coley.recaf.config.ConfigContainer;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Component for {@code boolean} config values where an action should be run when the value updates.
 *
 * @author Matt Coley
 */
public class ConfigActionableBoolean extends ConfigBoolean {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 * @param text
	 * 		Text to represent the field.
	 * @param handler
	 * 		Action to run on  value update.
	 */
	public ConfigActionableBoolean(ConfigContainer instance, Field field,
								   ObservableValue<String> text, Consumer<Boolean> handler) {
		super(instance, field, text);
		selectedProperty().addListener((observable, oldValue, newValue) -> handler.accept(newValue));
	}
}
