package software.coley.recaf.services.phantom;

/**
 * Exception thrown when {@link PhantomGenerator} operations fail.
 *
 * @author Matt Coley
 */
public class PhantomGenerationException extends Exception {
	/**
	 * @param cause
	 * 		The cause of the exception.
	 * @param message
	 * 		Detail message.
	 */
	public PhantomGenerationException(Throwable cause, String message) {
		super(message, cause);
	}

	/**
	 * @param cause
	 * 		The cause of the exception.
	 */
	public PhantomGenerationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * 		Detail message.
	 */
	public PhantomGenerationException(String message) {
		super(message);
	}
}
