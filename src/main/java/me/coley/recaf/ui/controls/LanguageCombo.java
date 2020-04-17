package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.util.Resource;
import me.coley.recaf.util.self.SelfReferenceUtil;

import java.util.List;

import static me.coley.recaf.util.Log.error;

/**
 * Language combobox.
 *
 * @author Matt
 */
public class LanguageCombo extends ComboBox<Resource> {
	/**
	 * @param wrapper
	 * 		wrapper for the language config field.
	 */
	public LanguageCombo(FieldWrapper wrapper) {
		// Extract: translations/{name}.json
		setConverter(new StringConverter<Resource>() {
			@Override
			public String toString(Resource r) {
				String p = r.getPath();
				p = p.substring(p.lastIndexOf('/') + 1);
				return p.substring(0, p.length() - 5);
			}

			@Override
			public Resource fromString(String text) { return null; }
		});
		try {
			Resource value = wrapper.get();
			List<Resource> langs = SelfReferenceUtil.get().getLangs();
			getItems().addAll(langs);
			getSelectionModel().select(value);
			getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> wrapper.set(n));
			setMaxWidth(Double.MAX_VALUE);
		} catch(Exception ex) {
			error(ex, "Failed creating language selector");
		}
	}
}
