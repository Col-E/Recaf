package me.coley.recaf.debug;

/**
 * Generic exception for wrapping JDI class redefinition failures.
 *
 * @author Matt
 */
public class JdiRedefineException extends Exception {
	/**
	 * @param message
	 * 		Error message.
	 */
	public JdiRedefineException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		Wrapped / cause exception.
	 * @param message
	 * 		Error message.
	 */
	public JdiRedefineException(Throwable cause, String message) {
		super(message, cause);
	}
}
