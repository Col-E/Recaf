package me.coley.recaf.plugin.api;

/**
 * Common plugin class.
 *
 * @author Matt
 */
public interface BasePlugin {
	/**
	 * @return Plugin name.
	 */
	default String getName() {
		org.plugface.core.annotations.Plugin anno =
				getClass().getAnnotation(org.plugface.core.annotations.Plugin.class);
		if (anno == null)
			throw new IllegalStateException("Class '" + getClass().getName() +
					"' does not have an @Plugin annotation!");
		return anno.name();
	}

	/**
	 * @return Plugin version.
	 */
	String getVersion();

	/**
	 * @return Description of the plugin's functionality.
	 */
	String getDescription();
}
