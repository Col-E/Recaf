package dev.xdark.recaf.plugin;

import me.coley.recaf.io.ByteSource;

import java.io.IOException;
import java.util.Collection;

public interface PluginManager {
	/**
	 * @return Allocator to use for creating plugin instances.
	 */
	ClassAllocator getAllocator();

	/**
	 * @param source
	 * 		source to pick from.
	 *
	 * @return loader that is capable of loading a plugin from this source, or {@code null}, if none was found.
	 *
	 * @throws IOException
	 * 		if any I/O error occurs.
	 *
	 * @see PluginLoader#isSupported(ByteSource)
	 */
	PluginLoader getLoader(ByteSource source) throws IOException;

	/**
	 * @param source
	 * 		source to pick from.
	 *
	 * @return {@code true} if any loader supports loading plugin
	 * from the provided byte source.
	 *
	 * @throws IOException
	 * 		if any I/O error occurs.
	 *
	 * @see PluginLoader#isSupported(ByteSource)
	 */
	boolean isSupported(ByteSource source) throws IOException;

	/**
	 * @param name
	 * 		Name of the plugin.
	 *
	 * @return plugin by it's name or {@code null}, if not found.
	 */
	<T extends Plugin> PluginContainer<T> getPlugin(String name);

	/**
	 * @return collection of loaders.
	 */
	Collection<PluginLoader> getLoaders();

	/**
	 * @return collection of plugins.
	 */
	Collection<? super PluginContainer<? extends Plugin>> getPlugins();

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
	 *
	 * @return loaded plugin.
	 *
	 * @throws PluginLoadException
	 * 		if there was no loader for the given source,
	 * 		or plugin failed to load.
	 */
	default <T extends Plugin> PluginContainer<T> loadPlugin(ByteSource source) throws PluginLoadException {
		for (PluginLoader loader : getLoaders()) {
			try {
				// Skip unsupported sources
				if (!loader.isSupported(source))
					continue;
				// Load and record plugin container
				PluginContainer<T> container = loader.load(getAllocator(), source);
				return loadPlugin(container);
			} catch(IOException | UnsupportedSourceException ex) {
				throw new PluginLoadException("Could not load plugin due to an error", ex);
			}
		}
		throw new PluginLoadException("Plugin manager was unable to locate suitable loader for the source.");
	}

	/**
	 * Loads and registers a plugin.
	 *
	 * @param container
	 *        Container of a {@link Plugin}.
	 *
	 * @return loaded plugin.
	 *
	 * @throws PluginLoadException
	 * 		Plugin failed to load.
	 */
	<T extends Plugin> PluginContainer<T> loadPlugin(PluginContainer<T> container) throws PluginLoadException;

	/**
	 * Unloads a plugin.
	 *
	 * @param name
	 * 		name of the plugin.
	 */
	void unloadPlugin(String name);

	/**
	 * Unloads a plugin.
	 *
	 * @param plugin
	 * 		instance of the plugin.
	 */
	void unloadPlugin(Plugin plugin);

	/**
	 * Unloads a plugin.
	 *
	 * @param container
	 * 		container of the plugin.
	 */
	void unloadPlugin(PluginContainer<?> container);
}
