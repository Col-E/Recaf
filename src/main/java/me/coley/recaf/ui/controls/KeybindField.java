package me.coley.recaf.ui.controls;

import javafx.event.Event;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import me.coley.recaf.Recaf;
import me.coley.recaf.config.ConfKeybinding;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;

/**
 * TextField for updating keybinds.
 *
 * @author Matt
 */
public class KeybindField extends TextField {
	private final ConfKeybinding.Binding target;
	private final String name;
	private ConfKeybinding.Binding lastest;

	/**
	 * @param field
	 * 		Target field of the binding to modify.
	 */
	public KeybindField(FieldWrapper field) {
		this.target = field.get();
		if (target == null)
			throw new IllegalStateException("KeybindField's target binding must not be null!");
		this.name = field.isTranslatable() ? field.name() : field.key();
		getStyleClass().add("key-field");
		setText(target.toString());
		setPromptText(LangUtil.translate("binding.inputprompt.initial"));
		// Show prompt when focused, otherwise show current target binding text
		focusedProperty().addListener((v, old, focus) -> setText(focus ? null : target.toString()));
		// Listen for new binding
		setOnKeyPressed(e -> {
			e.consume();
			// Skip if no-name support for the character
			if(e.getCode().getName().equalsIgnoreCase("undefined"))
				return;
			// Set target to latest
			if(e.getCode() == KeyCode.ENTER) {
				update();
				return;
			}
			// Update latest
			lastest = ConfKeybinding.Binding.from(e);
			setPromptText(LangUtil.translate("binding.inputprompt.finish"));
			setText(null);
			conf().setIsUpdating(true);
		});
		// Disable text updating
		setOnKeyTyped(Event::consume);
	}

	private void update() {
		conf().setIsUpdating(false);
		target.clear();
		target.addAll(lastest);
		getParent().requestFocus();

		setText(target.toString());
		Log.info("Updating keybind '{}' to '{}'", name, target.toString());
	}

	private ConfKeybinding conf() {
		return Recaf.getController().config().keys();
	}
}
