package me.coley.recaf.compile;

import java.util.Objects;

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

	@Override
	public String toString() {
		return line + ":" + message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompilerDiagnostic that = (CompilerDiagnostic) o;
		return line == that.line && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(line, message);
	}
}
