package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
import software.coley.recaf.util.Effects;

/**
 * Factory for general {@code int} values.
 *
 * @author pvpb0t
 */
@ApplicationScoped
public class IntegerComponentFactory extends TypedConfigComponentFactory<Integer> {

    @Inject
    protected IntegerComponentFactory() {
        super(false, int.class);
    }

    @Nonnull
    @Override
    public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Integer> value) {
        Observable<Integer> observable = value.getObservable();
        TextField textField = new TextField();
        textField.setText(Integer.toString(observable.getValue()));
        textField.textProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                int newValueAsInt = Integer.parseInt(newValue);
                observable.setValue(newValueAsInt);
                textField.setEffect(null);
            } catch (NumberFormatException e) {
                textField.setEffect(Effects.ERROR_BORDER);
            }
        });
        textField.focusedProperty().addListener((ob, old, isFocused) -> {
            if (!isFocused && textField.getEffect() != null) {
                textField.setText(Integer.toString(observable.getValue()));
                textField.setEffect(null);
            }
        });

        return textField;
    }
}
