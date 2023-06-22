package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.Button;
import software.coley.observables.ObservableObject;

import java.util.function.Function;

/**
 * Toggle button for iterating through values of an {@link Enum} with icon mappings.
 *
 * @author Amejonah
 * @author Matt Coley
 */
public class BoundMultiToggleIcon<E extends Enum<?>> extends Button implements Tooltipable {
	/**
	 * @param enumClass
	 * 		Enum class.
	 * @param enumObservable
	 * 		Observable holding enum value.
	 * @param iconProvider
	 * 		Icon mapper from enum value to graphic.
	 */
	public BoundMultiToggleIcon(@Nonnull Class<E> enumClass,
								@Nonnull ObservableObject<E> enumObservable,
								@Nonnull Function<E, Node> iconProvider) {
		enumObservable.addChangeListener((ob, old, cur) -> {
			setGraphic(iconProvider.apply(enumObservable.getValue()));
		});
		setOnAction(e -> {
			int index = (enumObservable.getValue().ordinal() + 1) % enumClass.getEnumConstants().length;
			enumObservable.setValue(enumClass.getEnumConstants()[index]);
		});
	}

	/**
	 * @param enumClass
	 * 		Enum class.
	 * @param enumProperty
	 * 		Property holding enum value.
	 * @param iconProvider
	 * 		Icon mapper from enum value to graphic.
	 */
	public BoundMultiToggleIcon(@Nonnull Class<E> enumClass,
								@Nonnull Property<E> enumProperty,
								@Nonnull Function<E, Node> iconProvider) {
		graphicProperty().bind(Bindings.createObjectBinding(() ->
				iconProvider.apply(enumProperty.getValue()), enumProperty));
		setOnAction(e -> {
			int index = (enumProperty.getValue().ordinal() + 1) % enumClass.getEnumConstants().length;
			enumProperty.setValue(enumClass.getEnumConstants()[index]);
		});
	}
}
