package me.coley.recaf.util.struct;

/**
 * Model of exception that holds line information.
 */
public interface LineException {
	/**
	 * @return Line the parse error occurred on.
	 */
	int getLine();

	/**
	 * @return Error message.
	 */
	String getMessage();
}
