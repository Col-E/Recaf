package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
import software.coley.recaf.util.Effects;

/**
 * Factory for general {@code long} values.
 */
@ApplicationScoped
public class LongComponentFactory extends TypedConfigComponentFactory<Long> {

    @Inject
    protected LongComponentFactory() {
        super(false, long.class);
    }

    @Nonnull
    @Override
    public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Long> value) {
        Observable<Long> observable = value.getObservable();
        TextField textField = new TextField();
        textField.setText(Long.toString(observable.getValue()));
        textField.textProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                long newValueAsLong = Long.parseLong(newValue);
                observable.setValue(newValueAsLong);
                textField.setEffect(null);
            } catch (NumberFormatException e) {
                textField.setEffect(Effects.ERROR_BORDER);
            }
        });
        textField.focusedProperty().addListener((ob, old, isFocused) -> {
            if (!isFocused && textField.getEffect() != null) {
                textField.setText(Long.toString(observable.getValue()));
                textField.setEffect(null);
            }
        });

        return textField;
    }
}
