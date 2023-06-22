package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Simple compiler feedback wrapper.
 *
 * @author Matt Coley
 */
public class CompilerDiagnostic {
	private final int line;
	private final int column;
	private final String message;
	private final Level level;

	/**
	 * @param line
	 * 		Line the message applies to.
	 * @param column
	 * 		Column the message applies to within the line.
	 * @param message
	 * 		Message detailing the problem.
	 * @param level
	 * 		Diagnostic problem level.
	 */
	public CompilerDiagnostic(int line, int column, @Nonnull String message, @Nonnull Level level) {
		this.line = line;
		this.column = column;
		this.message = message;
		this.level = level;
	}

	/**
	 * @param line
	 * 		New line number.
	 *
	 * @return Copy of diagnostic, with changed line number.
	 */
	@Nonnull
	public CompilerDiagnostic withLine(int line) {
		return new CompilerDiagnostic(line, column, message, level);
	}

	/**
	 * @return Line the message applies to.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Column the message applies to within the line.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @return Message detailing the problem.
	 */
	@Nonnull
	public String getMessage() {
		return message;
	}

	/**
	 * @return Diagnostic problem level.
	 */
	@Nonnull
	public Level getLevel() {
		return level;
	}

	@Override
	public String toString() {
		return level.name() + " - " + line + ":" + message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CompilerDiagnostic other = (CompilerDiagnostic) o;

		if (line != other.line) return false;
		if (column != other.column) return false;
		if (!Objects.equals(message, other.message)) return false;
		return level == other.level;
	}

	@Override
	public int hashCode() {
		int result = line;
		result = 31 * result + column;
		result = 31 * result + message.hashCode();
		result = 31 * result + level.hashCode();
		return result;
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
