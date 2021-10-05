package dev.xdark.recaf.plugin;

/**
 * Exception indicating that {@link PluginManager} failed to load
 * a plugin.
 *
 * @author xDark
 */
public final class PluginLoadException extends Exception {

	/**
	 * Constructs a new exception.
	 */
	public PluginLoadException() {
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param message
	 * 		the detail message.
	 */
	public PluginLoadException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param message
	 * 		the detail message.
	 * @param cause
	 * 		the cause of the exception.
	 */
	public PluginLoadException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param cause
	 * 		the cause of the exception.
	 */
	public PluginLoadException(Throwable cause) {
		super(cause);
	}
}
