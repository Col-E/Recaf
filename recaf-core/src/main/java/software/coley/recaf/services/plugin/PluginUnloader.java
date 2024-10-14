package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

import java.util.stream.Stream;

/**
 * Plugin unload action.
 *
 * @author xDark
 */
public interface PluginUnloader {
	/**
	 * Unloads the plugins.
	 *
	 * @throws PluginException
	 * 		If any of the plugins failed to unload.
	 */
	void commit() throws PluginException;

	/**
	 * @return Plugin to be unloaded.
	 */
	@Nonnull
	PluginInfo unloadingPlugin();

	/**
	 * @return A collection of plugins that depend on the plugin to be unloaded.
	 *
	 * @apiNote These plugins will also get unloaded.
	 */
	@Nonnull
	Stream<PluginInfo> dependants();
}
