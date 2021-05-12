package me.coley.recaf.config;

import me.coley.recaf.config.container.DisplayConfig;
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
	private static final KeybindConfig keybinds = new KeybindConfig();

	/**
	 * @return Collection of all config container instances.
	 */
	public static Collection<ConfigContainer> containers() {
		return Arrays.asList(
				display,
				keybinds
		);
	}

	/**
	 * @return Display config instance.
	 */
	public static DisplayConfig display() {
		return display;
	}

	/**
	 * @return Keybind config instance.
	 */
	public static KeybindConfig keybinds() {
		return keybinds;
	}
}
