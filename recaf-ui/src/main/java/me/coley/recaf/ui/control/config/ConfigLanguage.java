package me.coley.recaf.ui.control.config;

import javafx.scene.control.*;
import javafx.util.StringConverter;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.WindowBase;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ComboBox for switching between {@link Lang#getLanguages() the available languages}.
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
		// Sort languages by display name alphabetically
		this.getItems().addAll(Lang.getLanguageKeys().stream()
				.sorted(Comparator.comparing(o -> Lang.get(o, "lang.name")))
				.collect(Collectors.toList()));

		// Select the default (currently loaded) language
		SingleSelectionModel<String> selectionModel = getSelectionModel();
		setConverter(new LanguageStringConverter());
		selectionModel.select(Lang.getCurrentLanguage());

		selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			// Canceling (seen below) re-selects the old value.
			// This checks for that case and skips showing a duplicate 'Are you sure?' prompt.
			if (newValue.equals(Lang.getCurrentLanguage())) {
				return;
			}

			Alert restartAlert = new Alert(Alert.AlertType.CONFIRMATION);
			ButtonType yesButton = new ButtonType(Lang.get(newValue, "dialog.confirm"), ButtonBar.ButtonData.YES);
			ButtonType noButton = new ButtonType(Lang.get(newValue, "dialog.cancel"), ButtonBar.ButtonData.NO);
			restartAlert.setContentText(Lang.get(newValue, "dialog.restart").replace("\\n", "\n"));
			// Remove the default cancel and OK buttons
			restartAlert.getDialogPane().getButtonTypes().clear();
			restartAlert.getDialogPane().getButtonTypes().addAll(noButton, yesButton);
			WindowBase.installStyle(restartAlert.getDialogPane().getStylesheets());

			Optional<ButtonType> result = restartAlert.showAndWait();
			if (result.isEmpty() || result.get() == noButton) {
				// Reselect the old value to keep the 'cancellation' consistent in the UI
				selectionModel.select(oldValue);
			} else if (result.get() == yesButton) {
				// Apply
				ReflectUtil.quietSet(instance, field, newValue);
			}
		});
	}

	private static class LanguageStringConverter extends StringConverter<String> {
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
