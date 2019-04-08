package me.coley.recaf.config.impl;

import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;

/**
 * Options for main-window keybinds. All keybinds are applied when the control key is held down.
 * 
 * @author Matt
 */
public class ConfKeybinds extends Config {
	/**
	 * Allow main-window keybinds for common actions.
	 */
	@Conf(category = "keybinds", key = "active")
	public boolean active = true;
	/**
	 * Create a save-state.
	 */
	@Conf(category = "keybinds", key = "savestate")
	public String save = "S";
	/**
	 * Export changes.
	 */
	@Conf(category = "keybinds", key = "export")
	public String export = "E";
	/**
	 * Open new file.
	 */
	@Conf(category = "keybinds", key = "open")
	public String open = "O";
	/**
	 * Open search window.
	 */
	@Conf(category = "keybinds", key = "search")
	public String search = "H";
	/**
	 * Open search window.
	 */
	@Conf(category = "keybinds", key = "copy")
	public String copy = "C";
	/**
	 * Open search window.
	 */
	@Conf(category = "keybinds", key = "paste")
	public String paste = "V";
	/**
	 * Open search for instruction window, or code-area.
	 */
	@Conf(category = "keybinds", key = "find")
	public String find = "F";

	public ConfKeybinds() {
		super("rc_keybinds");
		load();
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfKeybinds instance.
	 */
	public static ConfKeybinds instance() {
		return ConfKeybinds.instance(ConfKeybinds.class);
	}
}
