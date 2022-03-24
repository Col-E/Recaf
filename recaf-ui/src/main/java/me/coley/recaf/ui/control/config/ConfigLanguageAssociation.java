package me.coley.recaf.ui.control.config;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.LanguageAssociationListener;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * HBox for configuring associations between file extensions and {@link Language languages}.
 *
 * @author yapht
 */
public class ConfigLanguageAssociation extends VBox implements LanguageAssociationListener {
	private final Map<String, ComboBox<String>> combos = new HashMap<>();
	private final Label noAssociationsLabel = new Label(Lang.get("menu.association.none"));

	/**
	 * @param instance
	 * 		Config container holding the association map
	 * 		<i>({@link me.coley.recaf.config.container.EditorConfig#fileExtensionAssociations})</i>
	 * @param field
	 * 		The field reference to the map.
	 */
	public ConfigLanguageAssociation(ConfigContainer instance, Field field) {
		Map<String, String> associations = ReflectUtil.quietGet(instance, field);
		setSpacing(10);

		// Add all the current associations
		for (Map.Entry<String, String> entry : associations.entrySet()) {
			String fileExt = entry.getKey();
			String languageName = entry.getValue();
			add(fileExt, languageName);
		}

		if (associations.isEmpty()) {
			getChildren().add(noAssociationsLabel);
		}

		Languages.addAssociationListener(this);
	}

	private void add(String extension, String languageName) {
		HBox row = new HBox();
		row.setAlignment(Pos.CENTER_LEFT);
		row.setSpacing(50.f);
		row.setId(extension);

		Label label = new Label("." + extension);
		label.maxWidth(50.f);
		label.setPrefWidth(50.f);

		ComboBox<String> languages = combos.getOrDefault(extension, new ComboBox<>());

		languages.getSelectionModel().select(languageName);
		languages.getSelectionModel().selectedItemProperty().addListener(
				(observable, oldValue, newValue)
						-> Languages.setExtensionAssociation(extension, newValue)
		);
		languages.getItems().addAll(Languages.AVAILABLE_KEYS);

		combos.putIfAbsent(extension, languages);

		Button remove = new Button(Lang.get("menu.edit.remove"));
		remove.setOnMouseClicked((e) -> removeAssociation(extension));

		row.getChildren().addAll(label, languages, remove);
		getChildren().add(row);

		getChildren().remove(noAssociationsLabel);
	}

	private void removeAssociation(String extension) {
		Languages.removeExtensionAssociation(extension);
		getChildren().removeIf(item -> extension.equals(item.getId()));
		if (getChildren().isEmpty()) {
			getChildren().add(noAssociationsLabel);
		}
		combos.remove(extension);
	}

	@Override
	public void onAssociationChanged(String extension, Language newLanguage) {
		String languageName = newLanguage.getKey();
		if (combos.containsKey(extension)) {
			combos.get(extension).getSelectionModel().select(languageName);
		} else {
			add(extension, languageName);
		}
	}
}
