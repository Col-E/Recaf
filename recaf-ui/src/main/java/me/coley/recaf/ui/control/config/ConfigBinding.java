package me.coley.recaf.ui.control.config;

import javafx.event.Event;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.binds.Binding;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * TextField for updating keybinds.
 *
 * @author Matt Coley
 */
public class ConfigBinding extends TextField implements Unlabeled {
	private final Binding target;
	private Binding lastest;

	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigBinding(ConfigContainer instance, Field field) {
		this.target = ReflectUtil.quietGet(instance, field);
		if (target == null)
			throw new IllegalStateException("KeybindField's target binding must not be null!");
		getStyleClass().add("key-field");
		setText(target.toString());
		promptTextProperty().bind(Lang.getBinding("conf.binding.inputprompt.initial"));
		// Show prompt when focused, otherwise show current target binding text
		focusedProperty().addListener((v, old, focus) -> setText(focus ? null : target.toString()));
		// Listen for new binding
		setOnKeyPressed(e -> {
			e.consume();
			// Skip if no-name support for the character
			if (e.getCode().getName().equalsIgnoreCase("undefined"))
				return;
			// Set target to latest
			if (e.getCode() == KeyCode.ENTER) {
				update();
				return;
			}
			// Update latest
			lastest = Binding.newBind(e);
			promptTextProperty().bind(Lang.getBinding("conf.binding.inputprompt.finish"));
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
	}

	private KeybindConfig conf() {
		return Configs.keybinds();
	}
}