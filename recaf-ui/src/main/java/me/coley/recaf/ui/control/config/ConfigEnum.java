package me.coley.recaf.ui.control.config;

import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between different enum options.
 *
 * @author Wolfie / win32kbase
 */
public class ConfigEnum extends ComboBox<String> implements Unlabeled {
    /**
     * @param instance
     * 		Config container.
     * @param field
     * 		Config field.
     */
    public ConfigEnum(ConfigContainer instance, Field field) {
        // Add all enum constant type names
        Object[] enumConstants = field.getType().getEnumConstants();
        for (Object enumObject : enumConstants) {
            getItems().add(enumObject.toString());
        }

        SingleSelectionModel<String> selectionModel = getSelectionModel();
        selectionModel.selectFirst();

        selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
                ReflectUtil.quietSet(instance, field, enumConstants[selectionModel.getSelectedIndex()]));
    }
}
