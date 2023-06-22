package software.coley.recaf.services.phantom;

/**
 * Exception thrown when {@link PhantomGenerator} operations fail.
 *
 * @author Matt Coley
 */
public class PhantomGenerationException extends Exception {
	/**
	 * @param cause
	 * 		Root cause of the failure.
	 * @param message
	 * 		Additional detail message.
	 */
	public PhantomGenerationException(Throwable cause, String message) {
		super(message, cause);
	}

	/**
	 * @param cause
	 * 		Root cause of the failure.
	 */
	public PhantomGenerationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * 		Additional detail message.
	 */
	public PhantomGenerationException(String message) {
		super(message);
	}
}
