package me.coley.recaf.util.visitor;

/**
 * Thrown when {@link ValidationClassReader} fails
 * to validate class file.
 *
 * @author xDark
 */
public final class QuietValidationException extends RuntimeException {

	public static final QuietValidationException INSTANCE = new QuietValidationException();

	private QuietValidationException() {
		super(null, null, false, false);
	}
}
