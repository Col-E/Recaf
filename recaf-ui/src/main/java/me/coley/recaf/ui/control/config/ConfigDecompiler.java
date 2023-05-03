package me.coley.recaf.ui.control.config;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.DecompilerCombo;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * ComboBox for switching between available decompiler implementations.
 * Updates the config of preferred decompiler.
 *
 * @author Wolfie / win32kbase
 */
public class ConfigDecompiler extends DecompilerCombo implements Unlabeled {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigDecompiler(ConfigContainer instance, Field field) {
		// Select the default (currently loaded) decompiler
		String initialDecompilerName = ReflectUtil.quietGet(instance, field).toString();
		select(initialDecompilerName);

		getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
				ReflectUtil.quietSet(instance, field, newValue.getName()));
	}
}
