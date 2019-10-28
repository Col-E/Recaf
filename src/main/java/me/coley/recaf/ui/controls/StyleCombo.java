package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.SelfReferenceUtil;

import java.util.List;

import static me.coley.recaf.util.Log.error;

/**
 * Style combobox.
 *
 * @author Matt
 */
public class StyleCombo extends ComboBox<String> {
	/**
	 * @param controller
	 * 		Controller context to update styles of.
	 * @param wrapper
	 * 		wrapper for the style config field.
	 */
	public StyleCombo(GuiController controller, FieldWrapper wrapper) {
		String value = wrapper.get();
		try {
			List<String> langs = SelfReferenceUtil.get().getStyles();
			getItems().addAll(langs);
			getSelectionModel().select(value);
			getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
				wrapper.set(n);
				controller.windows().reapplyStyles();
			});
			setMaxWidth(Double.MAX_VALUE);
		} catch(Exception ex) {
			error(ex, "Failed creating style selector");
		}
	}
}
