package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.compile.CompilerDiagnostic;

/**
 * Outline of a problem.
 *
 * @author Matt Coley
 */
public record Problem(int line, int column, int length, ProblemLevel level, ProblemPhase phase,
					  String message) implements Comparable<Problem> {

	/**
	 * @param diagnostic Compiler diagnostic message to adapt.
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
	 * @param newLine New line for problem.
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
		return Integer.compare(line, o.line);
	}
}
