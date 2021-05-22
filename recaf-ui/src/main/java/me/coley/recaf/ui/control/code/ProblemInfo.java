package me.coley.recaf.ui.control.code;

/**
 * Base problem information type.
 *
 * @author Matt Coley
 */
public class ProblemInfo {
	private final ProblemLevel level;
	private final String message;

	/**
	 * @param level
	 * 		Severity of problem.
	 * @param message
	 * 		Problem description.
	 */
	public ProblemInfo(ProblemLevel level, String message) {
		this.level = level;
		this.message = message;
	}

	/**
	 * @return Severity of problem.
	 */
	public ProblemLevel getLevel() {
		return level;
	}

	/**
	 * @return Problem description.
	 */
	public String getMessage() {
		return message;
	}
}
