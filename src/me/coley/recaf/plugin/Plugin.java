package me.coley.recaf.plugin;

public interface Plugin {
	/**
	 * Called by the plugin manager when loaded.
	 */
	default void init() {}

	/**
	 * Called when the plugin is clicked on in the plugins menu.
	 */
	default void onMenuClick() {}

	/**
	 * Identifier of the plugin.
	 */
	default String getID() {
		return this.getClass().getSimpleName();
	}
}
