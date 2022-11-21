package dev.xdark.recaf.plugin;

import me.coley.recaf.io.ByteSource;

import java.io.IOException;

/**
 * The plugin loader.
 * This interface is responsible for loading plugins
 * from different sources.
 *
 * @author xDark
 */
public interface PluginLoader {

	/**
	 * Loads a plugin from {@link ByteSource}.
	 *
	 * @param allocator
	 * 		Allocator to initialize {@link Plugin} instances with.
	 * @param in
	 *        {@link ByteSource} to load plugin from.
	 *
	 * @return Loaded plugin.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 * @throws PluginLoadException
	 * 		if loader has failed to load a plugin.
	 * @throws UnsupportedSourceException
	 * 		If loader does not support this type of source.
	 */
	<T extends Plugin> PluginContainer<T> load(ClassAllocator allocator, ByteSource in) throws IOException, PluginLoadException, UnsupportedSourceException;

	boolean isSupported(ByteSource source) throws IOException;

	/**
	 * Enables a plugin.
	 *
	 * @param container
	 * 		the container containing {@link Plugin} instance.
	 */
	void enablePlugin(PluginContainer<?> container);

	/**
	 * Disables a plugin.
	 *
	 * @param container
	 * 		the container containing {@link Plugin} instance.
	 */
	void disablePlugin(PluginContainer<?> container);
}
