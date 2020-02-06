package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.Themes;
import me.coley.recaf.util.Resource;

import static me.coley.recaf.util.Log.error;

/**
 * Style combobox.
 *
 * @author Matt
 */
public class StyleCombo extends ComboBox<Resource> {
	/**
	 * @param controller
	 * 		Controller context to update styles of.
	 * @param wrapper
	 * 		wrapper for the style config field.
	 */
	public StyleCombo(GuiController controller, FieldWrapper wrapper) {
		// Extract: style/ui-{name}.css
		setConverter(new StringConverter<Resource>() {
			@Override
			public String toString(Resource r) {
				String p = r.getPath();
				p = p.substring(p.lastIndexOf('/') + 1);
				return p.substring(3, p.length() - 4);
			}

			@Override
			public Resource fromString(String text) { return null; }
		});
		try {
			Resource value = wrapper.get();
			getItems().addAll(Themes.getStyles());
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
