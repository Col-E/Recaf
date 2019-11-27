package me.coley.recaf.ui.controls;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import me.coley.recaf.config.FieldWrapper;

/**
 * ComboBox for enumeration types.
 *
 * @param <E>
 * 		Enumeration type.
 *
 * @author Matt
 */
public class EnumComboBox<E extends Enum> extends ComboBox<E> {
	/**
	 * @param type
	 * 		Enumeration type.
	 * @param initial
	 * 		Initial selection.
	 */
	public EnumComboBox(Class<E> type, E initial) {
		super(FXCollections.observableArrayList(type.getEnumConstants()));
		setValue(initial);
	}

	/**
	 * @param wrapper
	 * 		Wrapper of an enumeration field.
	 */
	public EnumComboBox(FieldWrapper wrapper) {
		this(wrapper.type(), wrapper.get());
		getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> wrapper.set(n));
	}
}