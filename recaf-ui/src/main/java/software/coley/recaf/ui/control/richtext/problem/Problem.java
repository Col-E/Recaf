package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.compile.CompilerDiagnostic;

/**
 * Outline of a problem.
 *
 * @param line
 * 		Line the problem occurred on.
 * @param column
 * 		Column in the line the problem occurred on.
 * 		May be negative if position information is not available.
 * @param length
 * 		Length beyond the column position where the message applies to.
 * 		May be negative if position information is not available.
 * @param level
 * 		Problem level.
 * @param phase
 * 		Problem phase, stating at what point in the process the problem occurred.
 * @param message
 * 		Problem message.
 *
 * @author Matt Coley
 */
public record Problem(int line, int column, int length, ProblemLevel level, ProblemPhase phase,
                      String message) implements Comparable<Problem> {

	/**
	 * @param diagnostic
	 * 		Compiler diagnostic message to adapt.
	 *
	 * @return Problem from the diagnostic data.
	 */
	@Nonnull
	public static Problem fromDiagnostic(@Nonnull CompilerDiagnostic diagnostic) {
		ProblemLevel level = switch (diagnostic.level()) {
			case WARNING -> ProblemLevel.WARN;
			case INFO -> ProblemLevel.INFO;
			default -> ProblemLevel.ERROR;
		};
		return new Problem(diagnostic.line(), diagnostic.column(), diagnostic.length(),
				level, ProblemPhase.BUILD, diagnostic.message());
	}

	/**
	 * @param newLine
	 * 		New line for problem.
	 *
	 * @return Copy of the current problem, but with the line number modified.
	 */
	@Nonnull
	public Problem withLine(int newLine) {
		return new Problem(newLine, column, length, level, phase, message);
	}

	@Override
	public String toString() {
		return line + ":" + level.name() + ": " + message;
	}

	@Override
	public int compareTo(Problem o) {
		int cmp = Integer.compare(line, o.line);
		if (cmp == 0)
			cmp = Integer.compare(column, o.column);
		if (cmp == 0)
			cmp = level.compareTo(o.level);
		return cmp;
	}
}
