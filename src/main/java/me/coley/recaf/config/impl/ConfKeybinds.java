package me.coley.recaf.config.impl;

import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;
import me.coley.recaf.event.RequestAgentSaveEvent;
import me.coley.recaf.event.RequestSaveStateEvent;
import me.coley.recaf.ui.FileChoosers;
import me.coley.recaf.ui.FxSearch;

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

	@Conf(category = "keybinds", key = "duplicate")
	public String duplicate = "D";

	@Conf(category = "keybinds", key = "shift_up")
	public String shift_up = "Up";

	@Conf(category = "keybinds", key = "shift_down")
	public String shift_down = "Down";

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

	public void registerStage(Stage stage) {
		stage.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
			if (active && !e.isControlDown()) {
				return;
			}
			String code = e.getCode().getName();
			if (code.equalsIgnoreCase(save)) {
				Bus.post(new RequestSaveStateEvent());
			} else if (code.equalsIgnoreCase(export)) {
				if (Agent.isActive()) {
					Bus.post(new RequestAgentSaveEvent());
				} else {
					FileChoosers.export();
				}
			} else if (code.equalsIgnoreCase(open)) {
				FileChoosers.open();
			} else if (code.equalsIgnoreCase(search)) {
				FxSearch.open();
			}
		});
	}
}
