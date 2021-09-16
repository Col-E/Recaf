package me.coley.recaf.config;

import me.coley.recaf.config.container.DialogConfig;
import me.coley.recaf.config.container.DisplayConfig;
import me.coley.recaf.config.container.EditorConfig;
import me.coley.recaf.config.container.KeybindConfig;

import java.util.Arrays;
import java.util.Collection;

/**
 * Instance manager of {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
public class Configs {
	private static final DisplayConfig display = new DisplayConfig();
	private static final EditorConfig editor = new EditorConfig();
	private static final KeybindConfig keybinds = new KeybindConfig();
	private static final DialogConfig dialogs = new DialogConfig();

	/**
	 * @return Collection of all config container instances.
	 */
	public static Collection<ConfigContainer> containers() {
		return Arrays.asList(
				display,
				editor,
				keybinds,
				dialogs
		);
	}

	/**
	 * @return Display config instance.
	 */
	public static DisplayConfig display() {
		return display;
	}

	/**
	 * @return Editor config instance.
	 */
	public static EditorConfig editor() {
		return editor;
	}

	/**
	 * @return Keybind config instance.
	 */
	public static KeybindConfig keybinds() {
		return keybinds;
	}

	/**
	 * @return Dialog config instance.
	 */
	public static DialogConfig dialogs() {
		return dialogs;
	}
}
