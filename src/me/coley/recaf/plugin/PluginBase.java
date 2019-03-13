package me.coley.recaf.plugin;

/**
 * Common plugin class.
 * 
 * @author Matt
 */
public interface PluginBase {
	/**
	 * @return Plugin identifier.
	 */
	String name();

	/**
	 * @return Plugin version.
	 */
	String version();

	/**
	 * Called after the plugin is loaded into the plugin manager.
	 */
	default void onLoad() {}
}
