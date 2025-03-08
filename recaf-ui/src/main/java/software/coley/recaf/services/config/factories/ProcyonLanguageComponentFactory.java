package software.coley.recaf.services.config.factories;

import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
import software.coley.recaf.util.ToStringConverter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for general {@link Language} values.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ProcyonLanguageComponentFactory extends TypedConfigComponentFactory<Language> {
	@Inject
	protected ProcyonLanguageComponentFactory() {
		super(false, Language.class);
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Language> value) {
		Map<String, Language> languageMap = Languages.all().stream()
				.collect(Collectors.toMap(Language::getName, Function.identity()));
		Observable<Language> observable = value.getObservable();
		ComboBox<Language> comboBox = new ComboBox<>();
		comboBox.setConverter(new ToStringConverter<>() {
			@Override
			public String toString(Language language) {
				return language.getName();
			}
		});
		comboBox.setCellFactory(c -> new ListCell<>() {
			@Override
			protected void updateItem(Language item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getName());
				}
			}
		});
		comboBox.getItems().addAll(Languages.all());
		comboBox.setValue(observable.getValue());
		comboBox.valueProperty().addListener((observableValue, oldValue, newValue) -> {
			observable.setValue(newValue);
		});
		return comboBox;
	}
}
