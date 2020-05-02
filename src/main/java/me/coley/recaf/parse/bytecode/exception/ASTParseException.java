package me.coley.recaf.parse.bytecode.exception;

import me.coley.recaf.util.struct.LineException;

/**
 * Parse error.
 *
 * @author Matt
 */
public class ASTParseException extends Exception implements LineException {
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

	@Override
	public int getLine() {
		return line;
	}
}
