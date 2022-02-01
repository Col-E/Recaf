package me.coley.recaf.ui.control.config;

import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import me.coley.recaf.RecafUI;
import me.coley.recaf.compile.CompilerManager;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between available compiler implementations.
 *
 * @author Wolfie / win32kbase
 */
public class ConfigCompiler extends ComboBox<String> implements Unlabeled {
    /**
     * @param instance
     * 		Config container.
     * @param field
     * 		Config field.
     */
    public ConfigCompiler(ConfigContainer instance, Field field) {
        CompilerManager compilerManager = RecafUI.getController().getServices().getCompilerManager();
        compilerManager.getRegisteredImpls().forEach(compiler -> this.getItems().add(compiler.getName()));

        // Select the default (currently loaded) compiler
        SingleSelectionModel<String> selectionModel = getSelectionModel();
        selectionModel.select(ReflectUtil.quietGet(instance, field).toString());

        selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
                ReflectUtil.quietSet(instance, field, newValue));
    }

}
