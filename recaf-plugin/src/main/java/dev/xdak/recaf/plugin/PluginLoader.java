package dev.xdak.recaf.plugin;

import java.io.IOException;
import java.io.InputStream;

/**
 * The plugin loader.
 * This interface is responsible for loading plugins
 * from different sources.
 *
 * @author xDark
 */
public interface PluginLoader {

    /**
     * Loads a plugin from {@link InputStream}.
     *
     * @param in {@link InputStream} to load plugin from.
     * @return loaded plugin.
     * @throws IOException                If any I/O error occurs.
     * @throws UnsupportedSourceException If loader does not support this type of source.
     */
    <T extends Plugin> PluginContainer<T> load(InputStream in) throws IOException, UnsupportedSourceException;

    /**
     * Checks whether the source is supported or not.
     * Be aware that stream must be capable of {@link InputStream#reset()} and {@link InputStream#mark(int)}.
     *
     * @param in source to check from
     * @return {@code true} if the source is supported.
     * @throws IOException If any I/O error occurs.
     * @see InputStream#markSupported()
     */
    boolean isSupported(InputStream in) throws IOException;

    /**
     * Disables a plugin.
     */
    void disablePlugin(Plugin plugin);
}
