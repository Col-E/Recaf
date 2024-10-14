package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;

/**
 * Prepared plugin. Used as an intermediate step in plugin loading between {@link DiscoveredPluginSource}
 * and {@link PluginContainer}.
 *
 * @author xDark
 * @see BasicPluginManager#loadPlugins(PluginDiscoverer)
 */
public interface PreparedPlugin {

	/**
	 * @return Plugin information.
	 */
	@Nonnull
	PluginInfo info();

	/**
	 * @return Plugin source.
	 */
	@Nonnull
	PluginSource pluginSource();

	/**
	 * @return Plugin class name.
	 */
	@Nonnull
	String pluginClassName();

	/**
	 * Called if {@link PluginManager} rejects this plugin.
	 *
	 * @throws PluginException
	 * 		If any exception occurs.
	 */
	void reject() throws PluginException;
}
