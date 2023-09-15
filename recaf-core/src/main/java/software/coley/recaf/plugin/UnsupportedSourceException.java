package software.coley.recaf.plugin;

/**
 * Exception indicating that the source is not supported by the {@link PluginLoader}.
 *
 * @author xDark
 */
public final class UnsupportedSourceException extends Exception {
	/**
	 * Constructs a new exception.
	 */
	public UnsupportedSourceException() {
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param message
	 * 		The detail message.
	 */
	public UnsupportedSourceException(String message) {
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
	public UnsupportedSourceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param cause
	 * 		The cause of the exception.
	 */
	public UnsupportedSourceException(Throwable cause) {
		super(cause);
	}
}
