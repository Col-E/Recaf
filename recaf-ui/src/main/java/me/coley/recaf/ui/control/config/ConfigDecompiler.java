package me.coley.recaf.ui.control.config;

import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between available decompiler implementations.
 *
 * @author Wolfie / win32kbase
 */
public class ConfigDecompiler extends ComboBox<String> implements Unlabeled {
    /**
     * @param instance
     * 		Config container.
     * @param field
     * 		Config field.
     */
    public ConfigDecompiler(ConfigContainer instance, Field field) {
        DecompileManager decompileManager = RecafUI.getController().getServices().getDecompileManager();
        decompileManager.getRegisteredImpls().forEach(decompiler -> this.getItems().add(decompiler.getName()));

        // Select the default (currently loaded) decompiler
        SingleSelectionModel<String> selectionModel = getSelectionModel();
        selectionModel.select(ReflectUtil.quietGet(instance, field).toString());

        selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
                ReflectUtil.quietSet(instance, field, newValue));
    }

}
