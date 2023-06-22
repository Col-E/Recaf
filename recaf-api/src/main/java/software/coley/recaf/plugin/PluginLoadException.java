package software.coley.recaf.plugin;

/**
 * Exception indicating that a {@link Plugin} could not be loaded.
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
	 * 		The detail message.
	 */
	public PluginLoadException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param message
	 * 		The detail message.
	 * @param cause
	 * 		The cause of the exception.
	 */
	public PluginLoadException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param cause
	 * 		The cause of the exception.
	 */
	public PluginLoadException(Throwable cause) {
		super(cause);
	}
}
