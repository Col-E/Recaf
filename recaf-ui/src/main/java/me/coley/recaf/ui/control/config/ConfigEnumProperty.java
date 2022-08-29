package me.coley.recaf.ui.control.config;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between different enum options wrapped by {@link ObjectProperty}.
 *
 * @author Amejonah
 */
public class ConfigEnumProperty extends EnumComboBox<Enum<?>> implements Unlabeled {
	private final ObjectProperty<Enum<?>> value = new SimpleObjectProperty<>();

	/**
	 * @param enumClass Class of the enum.
	 * @param instance  Config container.
	 * @param field     Config field.
	 */
	@SuppressWarnings("unchecked")
	public ConfigEnumProperty(Class<Enum<?>> enumClass, ConfigContainer instance, Field field) {
		super(enumClass, ((ObjectProperty<Enum<?>>) ReflectUtil.quietGet(instance, field)).get());
		value.bindBidirectional(ReflectUtil.quietGet(instance, field));
		value.addListener((observable, oldValue, newValue) -> getSelectionModel().select(newValue));
		getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->	value.set(newValue));
	}
}
