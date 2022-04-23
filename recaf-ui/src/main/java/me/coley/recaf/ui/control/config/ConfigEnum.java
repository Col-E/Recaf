package me.coley.recaf.ui.control.config;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between different enum options.
 *
 * @author Wolfie / win32kbase
 * @author Matt Coley
 */
public class ConfigEnum extends ComboBox<Enum<?>> implements Unlabeled {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	@SuppressWarnings("unchecked")
	public ConfigEnum(ConfigContainer instance, Field field) {
		// Wrap constants to list
		ObservableList<Enum<?>> objects = (ObservableList<Enum<?>>)
				FXCollections.observableArrayList(field.getType().getEnumConstants());
		setItems(objects);
		// Register selection updates --> field setting
		// and select the current value.
		SingleSelectionModel<Enum<?>> selectionModel = getSelectionModel();
		selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
				ReflectUtil.quietSet(instance, field, newValue));
		Enum<?> current = ReflectUtil.quietGet(instance, field);
		getSelectionModel().select(current);
	}
}
