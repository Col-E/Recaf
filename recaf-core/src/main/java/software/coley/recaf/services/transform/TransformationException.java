package software.coley.recaf.services.transform;

/**
 * Thrown for any problem that prevents transformation operations.
 *
 * @author Matt Coley
 */
public class TransformationException extends Exception {
	/**
	 * @param message
	 * 		Detail message explaining the transformation failure.
	 */
	public TransformationException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * 		Detail message explaining the transformation failure.
	 * @param cause
	 * 		Root problem/cause.
	 */
	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}
}
