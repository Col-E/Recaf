package dev.xdark.recaf.plugin;

/**
 * Object that holds reference to a plugin and it's information.
 *
 * @author xDark
 */
public final class PluginContainer<T extends Plugin> {
	private final T plugin;
	private final PluginInformation information;
	private final PluginLoader loader;

	/**
	 * @param plugin
	 * 		plugin instance.
	 * @param information
	 * 		information about plugin.
	 * @param loader
	 * 		loader of the plugin.
	 *
	 * @see PluginInformation
	 * @see PluginLoader
	 */
	public PluginContainer(T plugin, PluginInformation information, PluginLoader loader) {
		this.plugin = plugin;
		this.information = information;
		this.loader = loader;
	}

	/**
	 * @return Plugin instance.
	 */
	public T getPlugin() {
		return plugin;
	}

	/**
	 * @return Information about plugin.
	 */
	public PluginInformation getInformation() {
		return information;
	}

	/**
	 * @return Loader of the plugin.
	 */
	public PluginLoader getLoader() {
		return loader;
	}
}
