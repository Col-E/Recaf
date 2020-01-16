package me.coley.recaf.parse.bytecode;

/**
 * Parse error.
 *
 * @author Matt
 */
public class ASTParseException extends Exception {
	private final int line;

	/**
	 * @param line
	 * 		Line the parse error occurred on.
	 * @param message
	 * 		Reason for parse failure.
	 */
	public ASTParseException(int line, String message) {
		this(null, line, message);
	}

	/**
	 * @param cause
	 * 		Cause exception.
	 * @param line
	 * 		Line the parse error occurred on.
	 * @param message
	 * 		Reason for parse failure.
	 */
	public ASTParseException(Exception cause, int line, String message) {
		super(message, cause);
		this.line = line;
	}

	/**
	 * @return Line the parse error occurred on.
	 */
	public int getLine() {
		return line;
	}
}
