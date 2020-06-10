package me.coley.recaf.debug;

/**
 * Generic exception for wrapping JDI method invocation failures.
 *
 * @author Matt
 */
public class JdiInvokeException extends Exception {
	/**
	 * @param message
	 * 		Error message.
	 */
	public JdiInvokeException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		Wrapped / cause exception.
	 * @param message
	 * 		Error message.
	 */
	public JdiInvokeException(Throwable cause, String message) {
		super(message, cause);
	}
}
