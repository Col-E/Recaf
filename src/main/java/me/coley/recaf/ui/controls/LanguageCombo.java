package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.util.SelfReferenceUtil;

import java.util.List;

import static me.coley.recaf.util.Log.error;

/**
 * Language combobox.
 *
 * @author Matt
 */
public class LanguageCombo extends ComboBox<String> {
	/**
	 * @param wrapper
	 * 		wrapper for the language config field.
	 */
	public LanguageCombo(FieldWrapper wrapper) {
		String value = wrapper.get();
		try {
			List<String> langs = SelfReferenceUtil.get().getLangs();
			getItems().addAll(langs);
			getSelectionModel().select(value);
			getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> wrapper.set(n));
			setMaxWidth(Double.MAX_VALUE);
		} catch(Exception ex) {
			error(ex, "Failed creating language selector");
		}
	}
}
