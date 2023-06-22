package software.coley.recaf.ui.control.richtext.problem;

/**
 * Phase in logic where the problem occurred.
 *
 * @author Matt Coley
 */
public enum ProblemPhase {
	/**
	 * Occurs prior to 'building' the content.
	 * <ul>
	 * <li>For Java, this would be analyzing the syntax and ensuring it is complaint, before attempting to compile.</li>
	 * <li>For bytecode, this would be building the AST model.</li>
	 * </ul>
	 */
	LINT,
	/**
	 * Occurs while 'building' the content.
	 * <ul>
	 * <li>For Java, this would be logic within {@code javac} handling failures.</li>
	 * <li>For bytecode, this would be converting the AST model to binary form.</li>
	 * </ul>
	 */
	BUILD,
	/**
	 * Occurs after content was built.
	 */
	POST_PROCESS
}
