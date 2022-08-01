package me.coley.recaf.ui.control.config;

import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between different enum options.
 *
 * @author Wolfie / win32kbase
 * @author Matt Coley
 */
public class ConfigEnum extends EnumComboBox<Enum<?>> implements Unlabeled {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	@SuppressWarnings("unchecked")
	public ConfigEnum(ConfigContainer instance, Field field) {
		super((Class<Enum<?>>) field.getType(), ReflectUtil.quietGet(instance, field));
		// Register selection updates --> field setting
		SingleSelectionModel<Enum<?>> selectionModel = getSelectionModel();
		selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
				ReflectUtil.quietSet(instance, field, newValue));
	}
}
