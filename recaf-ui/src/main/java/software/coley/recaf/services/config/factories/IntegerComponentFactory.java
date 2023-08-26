package software.coley.recaf.services.config.factories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
/**
 * Factory for general {@link Integer} values.
 *
 * @author pvpb0t
 * @since 8/25/2023
 */
@ApplicationScoped
public class IntegerComponentFactory extends TypedConfigComponentFactory<Integer> {

    @Inject
    protected IntegerComponentFactory() {
        super(false, Integer.class);
    }

    @Override
    public Node create(ConfigContainer container, ConfigValue<Integer> value) {
        Observable<Integer> observable = value.getObservable();
        String translationKey = container.getScopedId(value);
        TextField textField = new TextField();
        textField.setText(Integer.toString(observable.getValue()));
        textField.textProperty().addListener((observableValue, oldValue, newValue) -> {
            try {
                int newValueAsInt = Integer.parseInt(newValue);
                observable.setValue(newValueAsInt);
            } catch (NumberFormatException e) {
                textField.setText(oldValue);
            }
        });

        return textField;
    }

}
