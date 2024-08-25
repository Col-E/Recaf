package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;

/**
 * Simple compiler feedback wrapper.
 *
 * @param line
 * 		Line the message applies to.
 * @param column
 * 		Column the message applies to within the line.
 * @param length
 * 		Length beyond the column position where the message applies to.
 * @param message
 * 		Message detailing the problem.
 * @param level
 * 		Diagnostic problem level.
 *
 * @author Matt Coley
 */
public record CompilerDiagnostic(int line, int column, int length, @Nonnull String message, @Nonnull Level level) {
	/**
	 * @param line
	 * 		New line number.
	 *
	 * @return Copy of diagnostic, with changed line number.
	 */
	@Nonnull
	public CompilerDiagnostic withLine(int line) {
		return new CompilerDiagnostic(line, column, length, message, level);
	}

	@Override
	public String toString() {
		return level.name() + " on line " + line + ": " + message;
	}

	/**
	 * Diagnostic level.
	 */
	public enum Level {
		ERROR,
		WARNING,
		INFO
	}
}
