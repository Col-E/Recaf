package me.coley.recaf.plugin.api;

import me.coley.recaf.control.Controller;

public interface Startup extends PluginBase {
	/**
	 * Called before the controller is started, but after the controller is configured.
	 *
	 * @param controller
	 * 		The controller context Recaf was started with.
	 */
	void onStart(Controller controller);
}
