package me.coley.recaf.assemble;


import me.coley.recaf.assemble.ast.Element;

/**
 * Describes a problem that occurred with some AST.
 * These are irrecoverable problems that abort any further computation,
 * as opposed to {@link me.coley.recaf.assemble.validation.ValidationMessage}.
 *
 * @author Matt Coley
 */
public abstract class AstException extends Exception {
	private final Element source;

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param message
	 * 		Details of the problem.
	 */
	public AstException(Element source, String message) {
		this(source, null, message);
	}

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param cause
	 * 		Cause exception.
	 * @param message
	 * 		Details of the problem.
	 */
	public AstException(Element source, Throwable cause, String message) {
		super(message, cause);
		this.source = source;
	}


	/**
	 * @return Problematic AST node.
	 */
	public Element getSource() {
		return source;
	}

	/**
	 * @return Line of problem.
	 */
	public int getLine() {
		return getSource().getLine();
	}
}
