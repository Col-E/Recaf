package me.coley.recaf.compile;

/**
 * Simple compiler feedback wrapper.
 *
 * @author Matt Coley
 */
public class CompilerDiagnostic {
	private final int line;
	private final String message;

	/**
	 * @param line
	 * 		Line the message applies to.
	 * @param message
	 * 		Message detailing the problem.
	 */
	public CompilerDiagnostic(int line, String message) {
		this.line = line;
		this.message = message;
	}

	/**
	 * @return Line the message applies to.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Message detailing the problem.
	 */
	public String getMessage() {
		return message;
	}
}
