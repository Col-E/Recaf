package me.coley.recaf.ui.control.config;

import javafx.scene.control.*;
import javafx.util.StringConverter;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * ComboBox for switching between {@link Lang#getTranslations() the available translations}.
 *
 * @author Wolfie / win32kbase
 */
public class ConfigLanguage extends ComboBox<String> implements Unlabeled {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigLanguage(ConfigContainer instance, Field field) {
		// Sort translations by display name alphabetically
		getItems().addAll(Lang.getTranslationKeys().stream()
				.sorted(Comparator.comparing(o -> Lang.get(o, "lang.name")))
				.collect(Collectors.toList()));

		// Select the default (currently loaded) translations
		SingleSelectionModel<String> selectionModel = getSelectionModel();
		setConverter(new TranslationsStringConverter());
		selectionModel.select(Lang.getCurrentTranslations());

		selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			// Canceling (seen below) re-selects the old value.
			if (newValue.equals(Lang.getCurrentTranslations())) {
				return;
			}
			// Apply
			Lang.setCurrentTranslations(newValue);
			ReflectUtil.quietSet(instance, field, newValue);
		});
	}

	private static class TranslationsStringConverter extends StringConverter<String> {
		@Override
		public String toString(String object) {
			return Lang.get(object, "lang.name");
		}

		@Override
		public String fromString(String string) {
			return null;
		}
	}
}
