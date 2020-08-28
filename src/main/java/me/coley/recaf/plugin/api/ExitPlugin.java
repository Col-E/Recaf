package me.coley.recaf.plugin.api;

import me.coley.recaf.control.Controller;

/**
 * Allows plugins to be notified of Recaf's shutdown.
 *
 * @author xxDark
 */
public interface ExitPlugin extends BasePlugin {

    /**
     * Called before application is about to shutdown.
     *
     * @param controller
     * 		The controller context Recaf was started with.
     */
    void onExit(Controller controller);
}
