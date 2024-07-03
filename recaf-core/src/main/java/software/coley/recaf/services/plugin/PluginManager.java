package software.coley.recaf.services.plugin;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Outline of a plugin manager.
 *
 * @author xDark
 */
public interface PluginManager extends Service {
	String SERVICE_ID = "plugin-manager";

	/**
	 * @return Allocator to use for creating plugin instances.
	 */
	@Nonnull
	ClassAllocator getAllocator();

	/**
	 * @param id
	 * 		ID of the plugin.
	 * @param <T>
	 * 		Container plugin type.
	 *
	 * @return Plugin by its ID or {@code null}, if not found.
	 */
	@Nullable
	<T extends Plugin> PluginContainer<T> getPlugin(@Nonnull String id);

	/**
	 * @return Collection of plugins.
	 */
	@Nonnull
	Collection<PluginContainer<?>> getPlugins();

	/**
	 * @param type
	 * 		Some type to look for.
	 * @param <T>
	 * 		Requested type.
	 *
	 * @return Collection of plugins of the given type.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	default <T> Collection<T> getPluginsOfType(@Nonnull Class<T> type) {
		return (Collection<T>) getPlugins().stream()
				.map(PluginContainer::plugin)
				.filter(plugin -> type.isAssignableFrom(plugin.getClass()))
				.collect(Collectors.toList());
	}

	/**
	 * Checks if a plugin is loaded.
	 *
	 * @param id
	 * 		ID of plugin to check for.
	 *
	 * @return {@code true} if the plugin has been registered/loaded by this manager.
	 */
	boolean isPluginLoaded(@Nonnull String id);

	/**
	 * Registers new loader.
	 *
	 * @param loader
	 *        {@link PluginLoader} to register.
	 */
	void registerLoader(@Nonnull PluginLoader loader);

	/**
	 * @param discoverer
	 * 		Plugin discoverer.
	 *
	 * @return Loaded plugins.
	 *
	 * @throws PluginException
	 * 		If plugins fail to load.
	 */
	@Nonnull
	Collection<PluginContainer<?>> loadPlugins(@Nonnull PluginDiscoverer discoverer) throws PluginException;


	/**
	 * @param id
	 * 		ID of the plugin to be unloaded.
	 *
	 * @return Plugin unload action.
	 *
	 * @throws IllegalStateException
	 * 		If plugin to be unloaded was not found.
	 * @see PluginUnloader
	 */
	@Nonnull
	PluginUnloader unloaderFor(@Nonnull String id);
}
