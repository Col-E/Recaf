package software.coley.recaf.util.analysis.value;

/**
 * Exception thrown when creating a {@link ReValue} from some input fails.
 *
 * @author Matt Coley
 */
public class IllegalValueException extends Exception {
	/**
	 * @param message
	 * 		Reason why the value could not be created.
	 */
	public IllegalValueException(String message) {
		super(message);
	}
}
