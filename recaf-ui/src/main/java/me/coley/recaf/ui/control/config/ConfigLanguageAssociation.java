package me.coley.recaf.ui.control.config;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.LanguageAssociationListener;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HBox for configuring associations between file extensions and {@link Language languages}.
 *
 * @author yapht
 */
public class ConfigLanguageAssociation extends VBox implements LanguageAssociationListener {
	private final Map<String, ComboBox<Language>> combos = new HashMap<>();
	private final Label noAssociationsLabel = new Label();

	/**
	 * @param instance
	 * 		Config container holding the association map
	 * 		<i>({@link me.coley.recaf.config.container.EditorConfig#fileExtensionAssociations})</i>
	 * @param field
	 * 		The field reference to the map.
	 */
	public ConfigLanguageAssociation(ConfigContainer instance, Field field) {
		noAssociationsLabel.textProperty().bind(Lang.getBinding("menu.association.none"));
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

		Language language = Languages.getOrDefault(languageName, Languages.NONE);
		Collection<Language> sortedLangs = Languages.allLanguages().stream()
				.sorted(Comparator.comparing(Language::getName))
				.collect(Collectors.toList());

		ComboBox<Language> languageCombo = combos.getOrDefault(extension, new ComboBox<>());
		languageCombo.getSelectionModel().select(language);
		languageCombo.getSelectionModel().selectedItemProperty().addListener(
				(observable, oldValue, newValue)
						-> Languages.setExtensionAssociation(extension, newValue)
		);
		languageCombo.getItems().addAll(sortedLangs);
		languageCombo.setConverter(new StringConverter<>() {
			@Override
			public String toString(Language language) {
				return language.getName();
			}

			@Override
			public Language fromString(String string) {
				return Languages.allLanguages().stream()
						.filter(lang -> lang.getName().equals(string))
						.findFirst()
						.orElse(Languages.NONE);
			}
		});

		combos.putIfAbsent(extension, languageCombo);

		Button remove = new Button();
		remove.textProperty().bind(Lang.getBinding("menu.edit.remove"));
		remove.setOnMouseClicked((e) -> removeAssociation(extension));

		row.getChildren().addAll(label, languageCombo, remove);
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
			combos.get(extension).getSelectionModel().select(newLanguage);
		} else {
			add(extension, languageName);
		}
	}
}
