package me.coley.recaf.parse.bytecode;

/**
 * Exception for invalid assembler input.
 *
 * @author Matt
 */
public class AssemblerException extends Exception {
	private final int line;

	/**
	 * @param message
	 * 		Reason for assembler error.
	 */
	public AssemblerException(String message) {
		this(null, message, -1);
	}

	/**
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public AssemblerException(String message, int line) {
		this(null, message, line);
	}

	/**
	 * @param ex
	 * 		Cause exception.
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public AssemblerException(Exception ex, String message, int line) {
		super(message, ex);
		this.line = line;
	}

	/**
	 * @return Line number relevant to the error.
	 * May be {@code -1} if it is not specific to one line.
	 */
	public int getLine() {
		return line;
	}
}
