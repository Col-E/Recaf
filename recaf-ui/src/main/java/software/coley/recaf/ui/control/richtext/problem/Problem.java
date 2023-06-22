package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.compile.CompilerDiagnostic;

/**
 * Outline of a problem.
 *
 * @author Matt Coley
 */
public class Problem implements Comparable<Problem> {
	private final int line;
	private final int column;
	private final ProblemLevel level;
	private final ProblemPhase phase;
	private final String message;

	/**
	 * @param line
	 * 		Line the problem occurred on.
	 * @param column
	 * 		Column in the line the problem occurred on.
	 * 		May be negative if position information is not available.
	 * @param level
	 * 		Problem level.
	 * @param phase
	 * 		Problem phase, stating at what point in the process the problem occurred.
	 * @param message
	 * 		Problem message.
	 */
	public Problem(int line, int column, @Nonnull ProblemLevel level, @Nonnull ProblemPhase phase, @Nonnull String message) {
		this.line = line;
		this.column = column;
		this.level = level;
		this.phase = phase;
		this.message = message;
	}

	/**
	 * @param diagnostic
	 * 		Compiler diagnostic message to adapt.
	 *
	 * @return Problem from the diagnostic data.
	 */
	public static Problem fromDiagnostic(CompilerDiagnostic diagnostic) {
		ProblemLevel level = switch (diagnostic.getLevel()) {
			case WARNING -> ProblemLevel.WARN;
			case INFO -> ProblemLevel.INFO;
			default -> ProblemLevel.ERROR;
		};
		return new Problem(diagnostic.getLine(), diagnostic.getColumn(), level, ProblemPhase.BUILD, diagnostic.getMessage());
	}

	/**
	 * @param newLine
	 * 		New line for problem.
	 *
	 * @return Copy of the current problem, but with the line number modified.
	 */
	public Problem withLine(int newLine) {
		return new Problem(newLine, column, level, phase, message);
	}

	/**
	 * @param newColumn
	 * 		New column for problem.
	 *
	 * @return Copy of the current problem, but with the column number modified.
	 */
	public Problem withColumn(int newColumn) {
		return new Problem(line, newColumn, level, phase, message);
	}

	/**
	 * @return Line the problem occurred on.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Column in the line the problem occurred on.
	 * May be negative if position information is not available.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @return Problem level.
	 */
	@Nonnull
	public ProblemLevel getLevel() {
		return level;
	}

	/**
	 * @return Problem phase, stating at what point in the process the problem occurred.
	 */
	@Nonnull
	public ProblemPhase getPhase() {
		return phase;
	}

	/**
	 * @return Problem message.
	 */
	@Nonnull
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return line + ":" + level.name() + ": " + message;
	}

	@Override
	public int compareTo(Problem o) {
		return Integer.compare(line, o.line);
	}
}
