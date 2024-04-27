package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.PluginSource;
import software.coley.recaf.plugin.PluginException;
import software.coley.recaf.plugin.PluginInfo;

/**
 * Prepared plugin.
 *
 * @author xDark
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
	 * @throws PluginException If any exception occurs.
	 */
	void reject() throws PluginException;
}
