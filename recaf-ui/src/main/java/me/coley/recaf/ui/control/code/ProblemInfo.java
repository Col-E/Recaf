package me.coley.recaf.ui.control.code;

/**
 * Base problem information type.
 *
 * @author Matt Coley
 */
public class ProblemInfo implements Comparable<ProblemInfo> {
	private final ProblemOrigin origin;
	private final ProblemLevel level;
	private final int line;
	private final int column;
	private final String message;

	/**
	 * @param origin
	 * 		Where the problem originated from.
	 * @param level
	 * 		Severity of problem.
	 * @param line
	 * 		Line the problem occurred on.
	 * @param message
	 * 		Problem description.
	 */
	public ProblemInfo(ProblemOrigin origin, ProblemLevel level, int line, String message) {
		this(origin, level, line, -1, message);
	}

	/**
	 * @param origin
	 * 		Where the problem originated from.
	 * @param level
	 * 		Severity of problem.
	 * @param line
	 * 		Line the problem occurred on.
	 * @param column
	 * 		Column the problem occurred on.
	 * @param message
	 * 		Problem description.
	 */
	public ProblemInfo(ProblemOrigin origin, ProblemLevel level, int line, int column, String message) {
		this.origin = origin;
		this.level = level;
		this.line = line;
		this.column = column;
		this.message = message;
	}

	/**
	 * @return Where the problem originated from.
	 */
	public ProblemOrigin getOrigin() {
		return origin;
	}

	/**
	 * @return Severity of problem.
	 */
	public ProblemLevel getLevel() {
		return level;
	}

	/**
	 * @return Line the problem occurred on.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Column the problem occurred on.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @return Problem description.
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public int compareTo(ProblemInfo o) {
		return Integer.compare(line, o.line);
	}
}
