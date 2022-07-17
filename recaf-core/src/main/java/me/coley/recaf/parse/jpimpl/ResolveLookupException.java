package me.coley.recaf.parse.jpimpl;

/**
 * Exception type for failing to look up values for resolving logic.
 *
 * @author Matt Coley
 */
public class ResolveLookupException extends IllegalStateException {
	/**
	 * @param message
	 * 		Description of lookup failure.
	 */
	public ResolveLookupException(String message) {
		super(message);
	}
}
