package software.coley.recaf.plugin;

import jakarta.annotation.Nonnull;

/**
 * Object that holds reference to a plugin and it's information.
 *
 * @param <P>
 * 		Plugin instance type.
 *
 * @author xDark
 * @see PluginInfo
 * @see PluginLoader
 */
public final class PluginContainer<P extends Plugin> {
	private final P plugin;
	private final PluginInfo information;
	private final PluginLoader loader;

	/**
	 * @param plugin
	 * 		Plugin instance.
	 * @param information
	 * 		Information about plugin.
	 * @param loader
	 * 		Loader of the plugin.
	 */
	public PluginContainer(@Nonnull P plugin, @Nonnull PluginInfo information, @Nonnull PluginLoader loader) {
		this.plugin = plugin;
		this.information = information;
		this.loader = loader;
	}

	/**
	 * @return Plugin instance.
	 */
	@Nonnull
	public P getPlugin() {
		return plugin;
	}

	/**
	 * @return Information about plugin.
	 */
	@Nonnull
	public PluginInfo getInformation() {
		return information;
	}

	/**
	 * @return Loader of the plugin.
	 */
	@Nonnull
	public PluginLoader getLoader() {
		return loader;
	}
}
