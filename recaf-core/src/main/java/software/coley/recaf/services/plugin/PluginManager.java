package software.coley.recaf.services.plugin;


import software.coley.recaf.plugin.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.io.ByteSource;

import java.io.IOException;
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
	ClassAllocator getAllocator();

	/**
	 * @param source
	 * 		Source to pick from.
	 *
	 * @return Loader that is capable of loading a plugin from this source, or {@code null}, if none was found.
	 *
	 * @throws IOException
	 * 		When any I/O error occurs.
	 * @see PluginLoader#isSupported(ByteSource)
	 */
	PluginLoader getLoader(ByteSource source) throws IOException;

	/**
	 * @param source
	 * 		source to pick from.
	 *
	 * @return {@code true} if any loader supports loading plugin from the provided byte source.
	 *
	 * @throws IOException
	 * 		When any I/O error occurs.
	 * @see PluginLoader#isSupported(ByteSource)
	 */
	boolean isSupported(ByteSource source) throws IOException;

	/**
	 * @param name
	 * 		Name of the plugin.
	 * @param <T>
	 * 		Container plugin type.
	 *
	 * @return Plugin by its name or {@code null}, if not found.
	 */
	<T extends Plugin> PluginContainer<T> getPlugin(String name);

	/**
	 * @return Collection of loaders.
	 */
	Collection<PluginLoader> getLoaders();

	/**
	 * @return Collection of plugins.
	 */
	Collection<PluginContainer<? extends Plugin>> getPlugins();

	/**
	 * @param type
	 * 		Some type to look for.
	 * @param <T>
	 * 		Requested type.
	 *
	 * @return Collection of plugins of the given type.
	 */
	@SuppressWarnings("unchecked")
	default <T> Collection<T> getPluginsOfType(Class<T> type) {
		return (Collection<T>) getPlugins().stream()
				.map(PluginContainer::getPlugin)
				.filter(plugin -> type.isAssignableFrom(plugin.getClass()))
				.collect(Collectors.toList());
	}

	/**
	 * Registers new loader.
	 *
	 * @param loader
	 *        {@link PluginLoader} to register.
	 */
	void registerLoader(PluginLoader loader);

	/**
	 * Removes loader.
	 *
	 * @param loader
	 *        {@link PluginLoader} to remove.
	 */
	void removeLoader(PluginLoader loader);

	/**
	 * Loads and registers a plugin.
	 *
	 * @param source
	 *        {@link ByteSource} to load plugin from.
	 * @param <T>
	 * 		Container plugin type.
	 *
	 * @return Loaded plugin.
	 *
	 * @throws PluginLoadException
	 * 		When there was no loader for the given source, or the plugin failed to load.
	 */
	default <T extends Plugin> PluginContainer<T> loadPlugin(ByteSource source) throws PluginLoadException {
		for (PluginLoader loader : getLoaders()) {
			try {
				// Skip unsupported sources
				if (!loader.isSupported(source))
					continue;

				// Load and record plugin container
				PluginContainer<T> container = loader.load(getAllocator(), source);
				PluginContainer<T> loadedContainer = loadPlugin(container);
				if (shouldEnablePluginOnLoad(loadedContainer))
					loader.enablePlugin(loadedContainer);
				return loadedContainer;
			} catch (IOException | UnsupportedSourceException ex) {
				throw new PluginLoadException("Could not load plugin due to an error", ex);
			}
		}
		throw new PluginLoadException("Plugin manager was unable to locate suitable loader for the source.");
	}

	/**
	 * Loads and registers a plugin.
	 *
	 * @param container
	 * 		Container of a {@link Plugin}.
	 * @param <T>
	 * 		Container plugin type.
	 *
	 * @return Loaded plugin.
	 *
	 * @throws PluginLoadException
	 * 		Plugin failed to load.
	 */
	<T extends Plugin> PluginContainer<T> loadPlugin(PluginContainer<T> container) throws PluginLoadException;

	/**
	 * Unloads a plugin.
	 *
	 * @param name
	 * 		Name of the plugin.
	 */
	void unloadPlugin(String name);

	/**
	 * Unloads a plugin.
	 *
	 * @param plugin
	 * 		Instance of the plugin.
	 */
	void unloadPlugin(Plugin plugin);

	/**
	 * Unloads a plugin.
	 *
	 * @param container
	 * 		Container of the plugin.
	 */
	void unloadPlugin(PluginContainer<?> container);

	/**
	 * @param container
	 * 		Container of the plugin.
	 * @param <T>
	 * 		Container plugin type.
	 *
	 * @return {@code true} to enable the plugin once it is loaded.
	 */
	<T extends Plugin> boolean shouldEnablePluginOnLoad(PluginContainer<T> container);
}
