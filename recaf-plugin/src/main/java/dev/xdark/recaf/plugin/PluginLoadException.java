package dev.xdark.recaf.plugin;

/**
 * Exception indicating that {@link PluginManager} failed to load
 * a plugin.
 *
 * @author xDark
 */
public final class PluginLoadException extends Exception {

    public PluginLoadException() {
    }

    public PluginLoadException(String message) {
        super(message);
    }

    public PluginLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginLoadException(Throwable cause) {
        super(cause);
    }
}
