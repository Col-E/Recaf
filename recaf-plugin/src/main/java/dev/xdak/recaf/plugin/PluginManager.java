package dev.xdak.recaf.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface PluginManager {

    /**
     * @param in source to pick from.
     * @return loader that is capable of loading a plugin from this source,
     * or {@code null}, if none was found.o
     * @throws IOException if any I/O error occurs.
     * @see PluginLoader#isSupported(InputStream)
     */
    PluginLoader getLoader(InputStream in) throws IOException;

    /**
     * @param name Name of the plugin.
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
    Collection<? super Plugin> getPlugins();

    /**
     * Registers new loader.
     *
     * @param loader {@link PluginLoader} to register.
     */
    void registerLoader(PluginLoader loader);

    /**
     * Removes loader.
     *
     * @param loader {@link PluginLoader} to remove.
     */
    void removeLoader(PluginLoader loader);

    /**
     * Loads and registers a plugin.
     *
     * @param in {@link InputStream} to load plugin from.
     * @return loaded plugin.
     * @throws PluginLoadException if there was no loader for the given source,
     *                             or plugin failed to load.
     */
    <T extends Plugin> PluginContainer<T> loadPlugin(InputStream in) throws PluginLoadException;

    /**
     * Unloads a plugin.
     *
     * @param plugin name of the plugin.
     */
    void unloadPlugin(String plugin);

    /**
     * Unloads a plugin.
     *
     * @param plugin instance of the plugin.
     */
    void unloadPlugin(Object plugin);

    /**
     * Unloads a plugin.
     *
     * @param container container of the plugin.
     */
    void unloadPlugin(PluginContainer<?> container);
}
