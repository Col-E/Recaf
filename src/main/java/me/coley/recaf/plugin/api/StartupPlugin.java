package me.coley.recaf.plugin.api;

import me.coley.recaf.control.Controller;

/**
 * Allows plugins to be notified of Recaf's initialization.
 *
 * @author Matt
 */
public interface StartupPlugin extends BasePlugin {
	/**
	 * Called before the controller is started, but after the controller is configured.
	 *
	 * @param controller
	 * 		The controller context Recaf was started with.
	 */
	void onStart(Controller controller);
}
