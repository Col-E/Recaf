package me.coley.recaf.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.config.*;
import me.coley.recaf.control.gui.GuiController;

import java.util.*;
import java.util.function.Function;

/**
 * Pane for some config.
 *
 * @author Matt
 */
public class ConfigPane extends ColumnPane {
	private final Map<String, Function<FieldWrapper, Node>> editorOverrides = new HashMap<>();

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Display config.
	 */
	public ConfigPane(GuiController controller, ConfDisplay config) {
		editorOverrides.put("display.language", LanguageCombo::new);
		editorOverrides.put("display.textstyle", v -> new ThemeCombo(controller, v));
		editorOverrides.put("display.appstyle", v -> new StyleCombo(controller, v));
		editorOverrides.put("display.classmode", EnumComboBox::new);
		editorOverrides.put("display.filemode", EnumComboBox::new);
		setupConfigControls(config);
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Keybind config.
	 */
	public ConfigPane(GuiController controller, ConfKeybinding config) {
		editorOverrides.put("binding.close", KeybindField::new);
		editorOverrides.put("binding.saveapp", KeybindField::new);
		editorOverrides.put("binding.save", KeybindField::new);
		editorOverrides.put("binding.undo", KeybindField::new);
		setupConfigControls(config);
	}

	private void setupConfigControls(Config config) {
		for(FieldWrapper field : config.getConfigFields()) {
			// Skip hidden values
			if(field.hidden())
				continue;
			// Add label/editor
			SubLabeled label = new SubLabeled(field.name(), field.description());
			Node editor = editor(field);
			add(label, editor);
		}
	}

	private Node editor(FieldWrapper field) {
		// Check for override editor
		if (editorOverrides.containsKey(field.key()))
			return editorOverrides.get(field.key()).apply(field);
		// Create default editor
		return new TextField("TODO: Auto-editor");
	}
}
