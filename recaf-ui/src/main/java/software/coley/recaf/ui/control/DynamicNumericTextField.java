package software.coley.recaf.ui.control;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextFormatter;
import software.coley.recaf.util.NumberUtil;

/**
 * A numeric text field that can change the type of {@link Number} it holds.
 * Based on the text content, it can represent:
 * <ul>
 *     <li>{@link Integer}</li>
 *     <li>{@link Double}</li>
 *     <li>{@link Long}</li>
 *     <li>{@link Float}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class DynamicNumericTextField extends CustomTextField {
	private final Property<Number> numericValue = new SimpleObjectProperty<>();
	private final Property<Class<? extends Number>> numericType = new SimpleObjectProperty<>();

	/**
	 * New text field with properties to bind to.
	 *
	 * @param numericValueProperty
	 * 		Number value bind target.
	 * @param numericTypeProperty
	 * 		Number type bind target.
	 */
	public DynamicNumericTextField(@Nonnull ObjectProperty<Number> numericValueProperty,
								   @Nonnull ObjectProperty<Class<? extends Number>> numericTypeProperty) {
		this();

		numericValueProperty.bindBidirectional(numericValue);
		numericTypeProperty.bindBidirectional(numericType);

		setText(NumberUtil.toString(numericValueProperty.get()));
	}

	/**
	 * New empty text field.
	 */
	public DynamicNumericTextField() {
		textProperty().addListener((ob, old, cur) -> {
			if (cur.isEmpty()) {
				numericValue.setValue(0);
				numericType.setValue(Integer.class);
			} else {
				Number value = NumberUtil.parse(cur);
				numericValue.setValue(value);
				numericType.setValue(value.getClass());
			}
		});

		setTextFormatter(new TextFormatter<>(c -> {
			String newText = c.getControlNewText();
			if (newText.isEmpty())
				return c;
			try {
				NumberUtil.parse(newText);
				return c;
			} catch (NumberFormatException ex) {
				return null;
			}
		}));

		BoundLabel typeLabel = new BoundLabel(numericType.map(Class::getSimpleName));
		typeLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
		setRight(typeLabel);
	}
}
