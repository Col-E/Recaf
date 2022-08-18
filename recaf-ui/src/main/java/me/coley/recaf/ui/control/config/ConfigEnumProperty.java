package me.coley.recaf.ui.control.config;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

public class ConfigEnumProperty extends EnumComboBox<Enum<?>> implements Unlabeled {
	private final ObjectProperty<Enum<?>> value;
	private final ChangeListener<Enum<?>> listener = (observable, oldValue, newValue) -> {
		selectionModelProperty().get().select(newValue);
	};
	/**
	 * @param enumClass Class of the enum.
	 * @param instance  Config container.
	 * @param field     Config field.
	 */
	public ConfigEnumProperty(Class<Enum<?>> enumClass, ConfigContainer instance, Field field) {
		super(enumClass, ((ObjectProperty<Enum<?>>) ReflectUtil.quietGet(instance, field)).get());
		value = ReflectUtil.quietGet(instance, field);
		SingleSelectionModel<Enum<?>> selectionModel = getSelectionModel();
		selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
			value.set(newValue));
		value.addListener(new WeakChangeListener<>(listener));
	}
}
