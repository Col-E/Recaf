package me.coley.recaf.plugin.api;

import javafx.scene.input.KeyCode;
import me.coley.recaf.config.ConfKeybinding;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Allow plugins to register keybinds bound to certain actions.
 * <br>
 * See {@link ConfKeybinding.Binding#from(KeyCode...)} for easily creating bind instances.
 *
 * @author Matt
 */
public interface KeybindProviderPlugin extends BasePlugin {
	/**
	 * Create a map of keybinds for class-views.
	 *
	 * @return Generated binds, linked to their actions.
	 */
	default Map<ConfKeybinding.Binding, Runnable> createGlobalBindings() {
		return Collections.emptyMap();
	}

	/**
	 * Create a map of keybinds for class-views.
	 *
	 * @return Generated binds, linked to their actions.
	 */
	default Map<ConfKeybinding.Binding, Consumer<ClassViewport>> createClassViewBindings() {
		return Collections.emptyMap();
	}

	/**
	 * Create a map of keybinds for file-views.
	 *
	 * @return Generated binds, linked to their actions.
	 */
	default Map<ConfKeybinding.Binding, Consumer<FileViewport>> createFileViewBindings() {
		return Collections.emptyMap();
	}
}
