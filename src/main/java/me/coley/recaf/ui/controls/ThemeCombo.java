package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.SelfReferenceUtil;

import java.util.List;

import static me.coley.recaf.util.Log.error;

/**
 * Text theme combobox.
 *
 * @author Matt
 */
public class ThemeCombo extends ComboBox<String> {
	/**
	 * @param controller
	 * 		Controller context to update text theme of.
	 * @param wrapper
	 * 		wrapper for the style config field.
	 */
	public ThemeCombo(GuiController controller, FieldWrapper wrapper) {
		String value = wrapper.get();
		try {
			List<String> langs = SelfReferenceUtil.get().getTextThemes();
			getItems().addAll(langs);
			getSelectionModel().select(value);
			getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
				wrapper.set(n);
				controller.windows().reapplyStyles();
			});
			setMaxWidth(Double.MAX_VALUE);
		} catch(Exception ex) {
			error(ex, "Failed creating theme selector");
		}
	}
}
