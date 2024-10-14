package software.coley.recaf.services.plugin;

/**
 * Exception thrown when action involving plugins fail.
 *
 * @author xDark
 */
public final class PluginException extends Exception {
	/**
	 * Constructs a new exception.
	 */
	public PluginException() {
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param message
	 * 		The detail message.
	 */
	public PluginException(String message) {
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
	public PluginException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param cause
	 * 		The cause of the exception.
	 */
	public PluginException(Throwable cause) {
		super(cause);
	}
}
