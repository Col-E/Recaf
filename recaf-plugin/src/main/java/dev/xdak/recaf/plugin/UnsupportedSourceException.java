package dev.xdak.recaf.plugin;

/**
 * Exception indicating that the source is not supported
 * by the {@link PluginLoader}.
 *
 * @author xDark
 */
public final class UnsupportedSourceException extends Exception {

    public UnsupportedSourceException() {
    }

    public UnsupportedSourceException(String message) {
        super(message);
    }

    public UnsupportedSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedSourceException(Throwable cause) {
        super(cause);
    }
}
