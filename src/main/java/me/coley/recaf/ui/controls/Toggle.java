package me.coley.recaf.ui.controls;

import javafx.scene.control.CheckBox;
import me.coley.recaf.config.FieldWrapper;

/**
 * Checkbox for updating booleans.
 *
 * @author Matt
 */
public class Toggle extends CheckBox {
	/**
	 * @param field
	 * 		Target field of the boolean to modify.
	 */
	public Toggle(FieldWrapper field) {
		super("");
		setSelected(field.get());
		// Update field when box is ticked
		selectedProperty().addListener((v, old, current) -> {
			field.set(current);
		});
	}
}
