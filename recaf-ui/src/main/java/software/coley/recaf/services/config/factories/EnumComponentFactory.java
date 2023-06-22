package software.coley.recaf.services.config.factories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
import software.coley.recaf.ui.control.ObservableComboBox;

import java.util.Arrays;

/**
 * Factory for general {@link Enum} values.
 *
 * @author Matt Coley
 */
@ApplicationScoped
@SuppressWarnings("rawtypes")
public class EnumComponentFactory extends TypedConfigComponentFactory<Enum> {
	@Inject
	public EnumComponentFactory() {
		super(false, Enum.class);
	}

	@Override
	public Node create(ConfigContainer container, ConfigValue<Enum> value) {
		Enum[] enumConstants = value.getType().getEnumConstants();
		return new ObservableComboBox<>(value.getObservable(), Arrays.asList(enumConstants));
	}
}
